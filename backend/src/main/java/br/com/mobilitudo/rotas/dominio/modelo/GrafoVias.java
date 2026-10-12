package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Malha viaria carregada em memoria para um recorte geografico.
 *
 * <p>O grafo e dirigido: cada trecho bidirecional vira duas arestas, e a aresta de volta
 * carrega a inclinacao com o sinal trocado (ver {@link SegmentoVia#inverso()}). Trechos
 * de mao unica para pedestres existem em passarelas e rampas helicoidais, por isso a
 * direcionalidade e explicita em vez de presumida.</p>
 */
public final class GrafoVias {

    private final Map<String, NoVia> nos;
    private final Map<String, List<SegmentoVia>> arestasPorNo;

    private GrafoVias(Map<String, NoVia> nos, Map<String, List<SegmentoVia>> arestasPorNo) {
        this.nos = nos;
        this.arestasPorNo = arestasPorNo;
    }

    /** Monta o grafo tratando todos os trechos como bidirecionais. */
    public static GrafoVias de(Collection<SegmentoVia> segmentos) {
        return de(segmentos, List.of());
    }

    /**
     * Monta o grafo a partir dos trechos informados.
     *
     * @param segmentosBidirecionais trechos percorriveis nos dois sentidos
     * @param segmentosUnidirecionais trechos percorriveis apenas de origem para destino
     */
    public static GrafoVias de(Collection<SegmentoVia> segmentosBidirecionais,
                               Collection<SegmentoVia> segmentosUnidirecionais) {
        Map<String, NoVia> nos = new LinkedHashMap<>();
        Map<String, List<SegmentoVia>> arestas = new HashMap<>();

        for (SegmentoVia segmento : segmentosUnidirecionais) {
            registrar(nos, arestas, segmento);
        }
        for (SegmentoVia segmento : segmentosBidirecionais) {
            registrar(nos, arestas, segmento);
            registrar(nos, arestas, segmento.inverso());
        }
        return new GrafoVias(Map.copyOf(nos), Map.copyOf(arestas));
    }

    private static void registrar(Map<String, NoVia> nos,
                                  Map<String, List<SegmentoVia>> arestas,
                                  SegmentoVia segmento) {
        nos.putIfAbsent(segmento.origem().id(), segmento.origem());
        nos.putIfAbsent(segmento.destino().id(), segmento.destino());
        arestas.computeIfAbsent(segmento.origem().id(), chave -> new ArrayList<>()).add(segmento);
    }

    public List<SegmentoVia> arestasQuePartemDe(String idNo) {
        return arestasPorNo.getOrDefault(idNo, List.of());
    }

    public Optional<NoVia> no(String idNo) {
        return Optional.ofNullable(nos.get(idNo));
    }

    public Collection<NoVia> nos() {
        return nos.values();
    }

    public boolean vazio() {
        return nos.isEmpty();
    }

    /**
     * No da malha mais proximo de uma coordenada arbitraria, dentro do raio informado.
     *
     * <p>E o "snap" de origem e destino: o usuario aponta um endereco qualquer, e a rota
     * precisa comecar em um vertice que exista no grafo. Devolve vazio quando o ponto esta
     * fora da area mapeada, o que e informacao util e nao um erro.</p>
     */
    public Optional<NoVia> noMaisProximoDe(Coordenada coordenada, double raioMaximoMetros) {
        NoVia melhor = null;
        double menorDistancia = Double.MAX_VALUE;
        for (NoVia no : nos.values()) {
            double distancia = no.coordenada().distanciaEmMetrosAte(coordenada);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = no;
            }
        }
        return menorDistancia <= raioMaximoMetros ? Optional.ofNullable(melhor) : Optional.empty();
    }
}
