package br.com.mobilitudo.compartilhado.dominio;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AreaRetangularTest {

    @Test
    @DisplayName("A area envolve os dois pontos independentemente da ordem em que chegam")
    void envolveOsDoisPontosEmQualquerOrdem() {
        Coordenada norte = new Coordenada(-23.9920, -46.4110);
        Coordenada sul = new Coordenada(-23.9960, -46.4070);

        AreaRetangular direta = AreaRetangular.envolvendo(norte, sul, 0);
        AreaRetangular invertida = AreaRetangular.envolvendo(sul, norte, 0);

        assertThat(direta).isEqualTo(invertida);
        assertThat(direta.contem(norte)).isTrue();
        assertThat(direta.contem(sul)).isTrue();
    }

    @Test
    @DisplayName("A margem em metros vira a folga em graus que o desvio precisa")
    void aplicaMargemEmMetros() {
        Coordenada ponto = new Coordenada(-23.9930, -46.4100);

        AreaRetangular area = AreaRetangular.envolvendo(ponto, ponto, 600);

        // 600 m ao norte do canto sudoeste tem de cair dentro da area.
        assertThat(area.sudoeste().distanciaEmMetrosAte(new Coordenada(ponto.latitude(), area.sudoeste().longitude())))
                .isCloseTo(600, Offset.offset(5.0));
        assertThat(area.contem(new Coordenada(-23.9975, -46.4100))).isTrue();
        assertThat(area.contem(new Coordenada(-24.0100, -46.4100))).isFalse();
    }

    @Test
    @DisplayName("Cantos trocados sao recusados")
    void recusaCantosInvertidos() {
        assertThatThrownBy(() -> new AreaRetangular(
                new Coordenada(-23.9920, -46.4070),
                new Coordenada(-23.9960, -46.4110)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
