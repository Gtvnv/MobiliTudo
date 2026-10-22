package br.com.mobilitudo.rotas.dominio.servico;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.rotas.dominio.modelo.GrafoVias;
import br.com.mobilitudo.rotas.dominio.modelo.NoVia;
import br.com.mobilitudo.rotas.dominio.modelo.PassoRota;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.rotas.dominio.modelo.Rota;
import br.com.mobilitudo.rotas.dominio.modelo.RotaNaoEncontradaException;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Busca o caminho de menor custo de acessibilidade entre dois pontos.
 *
 * <p>Implementa A* sobre a malha viaria. A diferenca para um roteador comum esta na
 * funcao de custo: trechos intransponiveis para o perfil nem chegam a ser expandidos,
 * e os demais sao ponderados pelo esforco real de percorre-los. O resultado costuma ser
 * mais longo em metros e mais curto em barreiras, que e exatamente o objetivo.</p>
 *
 * <p>A heuristica e a distancia em linha reta ate o destino multiplicada pelo menor
 * fator de custo possivel do perfil. Isso a mantem admissivel mesmo com o bonus de
 * piso tatil, garantindo que o primeiro caminho encontrado seja otimo.</p>
 *
 * <p>Classe sem estado e sem dependencia de framework: e instanciavel em um teste
 * unitario comum, sem banco e sem contexto Spring.</p>
 */
public class CalculadoraRotaAcessivel {

    /** Distancia maxima aceitavel entre o ponto pedido e o no mais proximo da malha. */
    public static final double RAIO_DE_APROXIMACAO_METROS = 400;

    private static final double ANGULO_SEGUIR_EM_FRENTE = 25;
    private static final double ANGULO_CURVA_ACENTUADA = 120;

    private final AvaliadorAcessibilidade avaliador;
    private final double raioDeAproximacaoMetros;

    public CalculadoraRotaAcessivel() {
        this(new AvaliadorAcessibilidade(), RAIO_DE_APROXIMACAO_METROS);
    }

    public CalculadoraRotaAcessivel(AvaliadorAcessibilidade avaliador, double raioDeAproximacaoMetros) {
        this.avaliador = avaliador;
        this.raioDeAproximacaoMetros = raioDeAproximacaoMetros;
    }

    /**
     * Calcula a rota acessivel entre duas coordenadas arbitrarias.
     *
     * @throws RotaNaoEncontradaException quando um dos pontos esta fora da area mapeada
     *                                    ou quando nenhum caminho e transponivel pelo perfil
     */
    public Rota calcular(GrafoVias grafo, Coordenada origem, Coordenada destino, PerfilMobilidade perfil) {
        NoVia noOrigem = grafo.noMaisProximoDe(origem, raioDeAproximacaoMetros)
                .orElseThrow(() -> RotaNaoEncontradaException.origemForaDaArea(raioDeAproximacaoMetros));
        NoVia noDestino = grafo.noMaisProximoDe(destino, raioDeAproximacaoMetros)
                .orElseThrow(() -> RotaNaoEncontradaException.destinoForaDaArea(raioDeAproximacaoMetros));

        if (noOrigem.id().equals(noDestino.id())) {
            return rotaNoMesmoPonto(perfil, noOrigem);
        }

        List<SegmentoVia> trechos = buscar(grafo, noOrigem, noDestino, perfil);
        return montarRota(trechos, perfil);
    }

    /** A* propriamente dito: devolve a sequencia de trechos da origem ao destino. */
    private List<SegmentoVia> buscar(GrafoVias grafo, NoVia origem, NoVia destino, PerfilMobilidade perfil) {
        Map<String, Double> custoAcumulado = new HashMap<>();
        Map<String, SegmentoVia> trechoDeChegada = new HashMap<>();
        Set<String> fechados = new HashSet<>();

        PriorityQueue<NoAberto> abertos = new PriorityQueue<>(Comparator.comparingDouble(NoAberto::prioridade));
        custoAcumulado.put(origem.id(), 0.0);
        abertos.add(new NoAberto(origem, heuristica(origem, destino, perfil)));

        while (!abertos.isEmpty()) {
            NoAberto atual = abertos.poll();
            String idAtual = atual.no().id();

            if (idAtual.equals(destino.id())) {
                return reconstruirCaminho(trechoDeChegada, origem, destino);
            }
            // A fila pode guardar entradas obsoletas do mesmo no; a primeira saida e a definitiva.
            if (!fechados.add(idAtual)) {
                continue;
            }

            for (SegmentoVia trecho : grafo.arestasQuePartemDe(idAtual)) {
                if (!avaliador.transitavel(trecho, perfil)) {
                    continue;
                }
                String idVizinho = trecho.destino().id();
                if (fechados.contains(idVizinho)) {
                    continue;
                }
                double custoCandidato = custoAcumulado.get(idAtual) + avaliador.custo(trecho, perfil);
                if (custoCandidato < custoAcumulado.getOrDefault(idVizinho, Double.MAX_VALUE)) {
                    custoAcumulado.put(idVizinho, custoCandidato);
                    trechoDeChegada.put(idVizinho, trecho);
                    abertos.add(new NoAberto(trecho.destino(),
                            custoCandidato + heuristica(trecho.destino(), destino, perfil)));
                }
            }
        }
        throw RotaNaoEncontradaException.semCaminhoAcessivel(perfil);
    }

    private double heuristica(NoVia atual, NoVia destino, PerfilMobilidade perfil) {
        return atual.coordenada().distanciaEmMetrosAte(destino.coordenada()) * perfil.fatorMinimoDeCusto();
    }

    private List<SegmentoVia> reconstruirCaminho(Map<String, SegmentoVia> trechoDeChegada,
                                                 NoVia origem,
                                                 NoVia destino) {
        Deque<SegmentoVia> caminho = new ArrayDeque<>();
        String cursor = destino.id();
        while (!cursor.equals(origem.id())) {
            SegmentoVia trecho = trechoDeChegada.get(cursor);
            caminho.addFirst(trecho);
            cursor = trecho.origem().id();
        }
        return List.copyOf(caminho);
    }

    private Rota montarRota(List<SegmentoVia> trechos, PerfilMobilidade perfil) {
        List<PassoRota> passos = new ArrayList<>(trechos.size());
        double custoTotal = 0;

        for (int i = 0; i < trechos.size(); i++) {
            SegmentoVia trecho = trechos.get(i);
            SegmentoVia anterior = i == 0 ? null : trechos.get(i - 1);
            custoTotal += avaliador.custo(trecho, perfil);

            passos.add(new PassoRota(
                    instrucao(anterior, trecho, i == trechos.size() - 1),
                    trecho.nomeVia(),
                    trecho.id(),
                    trecho.comprimentoMetros(),
                    avaliador.nivel(trecho, perfil),
                    avaliador.ressalvas(trecho, perfil)));
        }
        return Rota.de(perfil, trechos, passos, custoTotal);
    }

    private Rota rotaNoMesmoPonto(PerfilMobilidade perfil, NoVia no) {
        PassoRota passo = new PassoRota("Você já está no destino.", "", "", 0,
                NivelAcessibilidade.PLENO, List.of());
        return new Rota(perfil, 0, 0, 0, NivelAcessibilidade.PLENO,
                List.of(passo), List.of(no.coordenada()));
    }

    private String instrucao(SegmentoVia anterior, SegmentoVia atual, boolean ultimo) {
        String distancia = "%d m".formatted(Math.round(atual.comprimentoMetros()));
        String via = atual.nomeVia();

        String movimento;
        if (anterior == null) {
            movimento = atual.travessia()
                    ? "Atravesse em %s".formatted(via)
                    : "Siga por %s".formatted(via);
        } else {
            movimento = "%s%s".formatted(manobra(anterior, atual), atual.travessia()
                    ? " e atravesse em %s".formatted(via)
                    : " em %s".formatted(via));
        }
        return ultimo
                ? "%s por %s. Você chegou ao destino.".formatted(movimento, distancia)
                : "%s por %s.".formatted(movimento, distancia);
    }

    /** Converte a variacao de azimute entre dois trechos em uma manobra descrita em texto. */
    private String manobra(SegmentoVia anterior, SegmentoVia atual) {
        double azimuteAnterior = anterior.origem().coordenada().azimuteAte(anterior.destino().coordenada());
        double azimuteAtual = atual.origem().coordenada().azimuteAte(atual.destino().coordenada());

        // Normaliza para (-180, 180]: negativo e esquerda, positivo e direita.
        double variacao = ((azimuteAtual - azimuteAnterior + 540) % 360) - 180;
        double modulo = Math.abs(variacao);

        if (modulo < ANGULO_SEGUIR_EM_FRENTE) {
            return "Siga em frente";
        }
        String lado = variacao > 0 ? "direita" : "esquerda";
        return modulo >= ANGULO_CURVA_ACENTUADA
                ? "Vire acentuadamente à %s".formatted(lado)
                : "Vire à %s".formatted(lado);
    }

    private record NoAberto(NoVia no, double prioridade) {
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "CalculadoraRotaAcessivel[raioAproximacao=%.0fm]",
                raioDeAproximacaoMetros);
    }
}
