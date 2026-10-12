package br.com.mobilitudo.rotas.infraestrutura.web;

import br.com.mobilitudo.compartilhado.infraestrutura.web.CoordenadaDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(name = "PedidoDeRota", description = "Origem, destino e perfil de mobilidade")
public record PedidoDeRotaDTO(
        @NotNull @Valid CoordenadaDTO origem,
        @NotNull @Valid CoordenadaDTO destino,
        @Schema(description = "CADEIRA_DE_RODAS, MOBILIDADE_REDUZIDA, DEFICIENCIA_VISUAL ou PADRAO",
                example = "CADEIRA_DE_RODAS")
        String perfil) {
}
