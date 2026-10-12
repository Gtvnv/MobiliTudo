package br.com.mobilitudo.compartilhado.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.data.Offset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordenadaTest {

    @Test
    @DisplayName("Haversine bate com a distancia conhecida entre dois pontos de Praia Grande")
    void calculaDistanciaGeodesica() {
        Coordenada palacioDasArtes = new Coordenada(-23.992, -46.411);
        Coordenada litoralPlaza = new Coordenada(-23.993, -46.407);

        assertThat(palacioDasArtes.distanciaEmMetrosAte(litoralPlaza))
                .isCloseTo(422, Offset.offset(10.0));
    }

    @Test
    @DisplayName("A distancia e simetrica e zero para o mesmo ponto")
    void distanciaESimetrica() {
        Coordenada a = new Coordenada(-23.992, -46.411);
        Coordenada b = new Coordenada(-24.015, -46.398);

        assertThat(a.distanciaEmMetrosAte(b)).isEqualTo(b.distanciaEmMetrosAte(a));
        assertThat(a.distanciaEmMetrosAte(a)).isZero();
    }

    @Test
    @DisplayName("Azimute aponta para o norte, leste, sul e oeste corretamente")
    void calculaAzimute() {
        Coordenada centro = new Coordenada(-23.9930, -46.4100);

        assertThat(centro.azimuteAte(new Coordenada(-23.9920, -46.4100))).isCloseTo(0, Offset.offset(1.0));
        assertThat(centro.azimuteAte(new Coordenada(-23.9930, -46.4090))).isCloseTo(90, Offset.offset(1.0));
        assertThat(centro.azimuteAte(new Coordenada(-23.9940, -46.4100))).isCloseTo(180, Offset.offset(1.0));
        assertThat(centro.azimuteAte(new Coordenada(-23.9930, -46.4110))).isCloseTo(270, Offset.offset(1.0));
    }

    @Test
    @DisplayName("Coordenada fora do globo e recusada na criacao")
    void recusaCoordenadaInvalida() {
        assertThatThrownBy(() -> new Coordenada(-91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordenada(0, 181)).isInstanceOf(IllegalArgumentException.class);
    }
}
