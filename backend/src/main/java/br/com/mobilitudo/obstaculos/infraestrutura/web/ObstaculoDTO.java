package br.com.mobilitudo.obstaculos.infraestrutura.web;

import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "Obstaculo", description = "Barreira urbana reportada pela comunidade")
public record ObstaculoDTO(UUID id,
                           String tipo,
                           String tipoDescricao,
                           String severidade,
                           List<Double> coords,
                           String descricao,
                           String segmentoViaId,
                           String reportadoPor,
                           Instant reportadoEm,
                           String status,
                           boolean afetaRotas) {

    public static ObstaculoDTO de(Obstaculo obstaculo) {
        return new ObstaculoDTO(
                obstaculo.id(),
                obstaculo.tipo().name(),
                obstaculo.tipo().descricao(),
                obstaculo.severidade().name(),
                List.of(obstaculo.coordenada().latitude(), obstaculo.coordenada().longitude()),
                obstaculo.descricao(),
                obstaculo.segmentoViaId().orElse(null),
                obstaculo.reportadoPor(),
                obstaculo.reportadoEm(),
                obstaculo.status().name(),
                obstaculo.afetaRoteamento());
    }
}
