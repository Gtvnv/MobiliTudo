package br.com.mobilitudo.rotas.infraestrutura.web;

import br.com.mobilitudo.rotas.dominio.modelo.PassoRota;
import br.com.mobilitudo.rotas.dominio.modelo.Rota;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Rota serializada para o mapa.
 *
 * <p>{@code geometria} vem no formato que o Leaflet consome direto em
 * {@code L.polyline}: pares [latitude, longitude].</p>
 */
@Schema(name = "Rota", description = "Trajeto calculado com suas instruções e ressalvas")
public record RotaDTO(String perfil,
                      String perfilDescricao,
                      double distanciaMetros,
                      double custoPonderado,
                      double duracaoMinutos,
                      String nivelAcessibilidade,
                      String nivelDescricao,
                      List<PassoDTO> passos,
                      List<String> avisos,
                      List<List<Double>> geometria) {

    public static RotaDTO de(Rota rota) {
        return new RotaDTO(
                rota.perfil().name(),
                rota.perfil().descricao(),
                arredondar(rota.distanciaTotalMetros()),
                arredondar(rota.custoPonderado()),
                arredondar(rota.duracaoEstimadaMinutos()),
                rota.nivelGeral().name(),
                rota.nivelGeral().descricao(),
                rota.passos().stream().map(PassoDTO::de).toList(),
                rota.avisos(),
                rota.geometria().stream()
                        .map(coordenada -> List.of(coordenada.latitude(), coordenada.longitude()))
                        .toList());
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    @Schema(name = "PassoRota", description = "Uma instrução de navegação")
    public record PassoDTO(String instrucao,
                           String via,
                           String segmentoId,
                           double distanciaMetros,
                           String nivelAcessibilidade,
                           List<String> ressalvas) {

        static PassoDTO de(PassoRota passo) {
            return new PassoDTO(
                    passo.instrucao(),
                    passo.nomeVia(),
                    passo.segmentoId(),
                    arredondar(passo.distanciaMetros()),
                    passo.nivel().name(),
                    passo.ressalvas());
        }
    }
}
