package br.com.mobilitudo.compartilhado.infraestrutura.web;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Par latitude/longitude em WGS 84")
public record CoordenadaDTO(
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        @Schema(example = "-23.9920") Double lat,

        @NotNull @DecimalMin("-180") @DecimalMax("180")
        @Schema(example = "-46.4110") Double lng) {

    public Coordenada paraDominio() {
        return new Coordenada(lat, lng);
    }

    public static CoordenadaDTO de(Coordenada coordenada) {
        return new CoordenadaDTO(coordenada.latitude(), coordenada.longitude());
    }
}
