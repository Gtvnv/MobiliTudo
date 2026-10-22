package br.com.mobilitudo.obstaculos.infraestrutura.web;

import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.compartilhado.infraestrutura.web.CoordenadaDTO;
import br.com.mobilitudo.obstaculos.aplicacao.GerenciarObstaculos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Entrada do relato de uma barreira urbana. */
public record NovoObstaculoDTO(
        @NotNull TipoObstaculo tipo,
        @NotNull Severidade severidade,
        @NotNull @Valid CoordenadaDTO coordenada,
        @Size(max = 1000) String descricao,
        @Size(max = 64) String segmentoViaId,
        @Size(max = 120) String reportadoPor) {

    public GerenciarObstaculos.Comando paraComando() {
        return new GerenciarObstaculos.Comando(
                tipo, severidade, coordenada.paraDominio(), descricao, segmentoViaId, reportadoPor);
    }
}
