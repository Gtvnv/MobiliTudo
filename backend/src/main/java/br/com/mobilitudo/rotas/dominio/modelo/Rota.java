package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Resultado do calculo: o caminho escolhido e por que ele foi escolhido.
 *
 * <p>Guarda tanto a distancia real quanto o custo ponderado. Os dois quase nunca
 * coincidem, e essa diferenca e justamente o ponto do projeto: a rota mais curta
 * raramente e a mais acessivel.</p>
 *
 * @param perfil             perfil de mobilidade usado no calculo
 * @param distanciaTotalMetros soma das extensoes dos trechos
 * @param custoPonderado     distancia ajustada pelas penalidades de acessibilidade
 * @param duracaoEstimadaMinutos tempo estimado na velocidade media do perfil
 * @param nivelGeral         pior nivel entre os trechos percorridos
 * @param passos             instrucoes de navegacao
 * @param geometria          polilinha para desenhar no mapa
 */
public record Rota(PerfilMobilidade perfil,
                   double distanciaTotalMetros,
                   double custoPonderado,
                   double duracaoEstimadaMinutos,
                   NivelAcessibilidade nivelGeral,
                   List<PassoRota> passos,
                   List<Coordenada> geometria) {

    public Rota {
        passos = List.copyOf(passos);
        geometria = List.copyOf(geometria);
    }

    public static Rota de(PerfilMobilidade perfil,
                          List<SegmentoVia> trechos,
                          List<PassoRota> passos,
                          double custoPonderado) {
        double distancia = trechos.stream().mapToDouble(SegmentoVia::comprimentoMetros).sum();
        NivelAcessibilidade nivelGeral = passos.stream()
                .map(PassoRota::nivel)
                .reduce(NivelAcessibilidade.PLENO, NivelAcessibilidade::pior);

        // LinkedHashSet remove o no compartilhado entre trechos consecutivos sem reordenar a linha.
        LinkedHashSet<Coordenada> pontos = new LinkedHashSet<>();
        trechos.forEach(trecho -> pontos.addAll(trecho.geometria()));

        double duracaoMinutos = distancia / perfil.velocidadeMediaMetrosPorSegundo() / 60.0;
        return new Rota(perfil, distancia, custoPonderado, duracaoMinutos, nivelGeral,
                passos, List.copyOf(pontos));
    }

    /** Todas as ressalvas dos trechos, na ordem em que aparecem no caminho. */
    public List<String> avisos() {
        return passos.stream().flatMap(passo -> passo.ressalvas().stream()).distinct().toList();
    }
}
