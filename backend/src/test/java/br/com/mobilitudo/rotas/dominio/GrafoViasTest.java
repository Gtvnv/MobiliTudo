package br.com.mobilitudo.rotas.dominio;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.rotas.dominio.modelo.GrafoVias;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.DESTINO;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.ESQUINA_NORTE;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.ORIGEM;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.calcadaBoa;
import static org.assertj.core.api.Assertions.assertThat;

class GrafoViasTest {

    @Test
    @DisplayName("Trecho bidirecional vira duas arestas, e a de volta e a mesma calcada")
    void bidirecionalGeraArestaDeVoltaComOMesmoId() {
        GrafoVias grafo = GrafoVias.de(List.of(calcadaBoa("av-1-2", ORIGEM, DESTINO).construir()));

        assertThat(grafo.arestasQuePartemDe("origem")).singleElement()
                .extracting(SegmentoVia::id).isEqualTo("av-1-2");
        // O id nao muda no sentido inverso: e o mesmo trecho fisico, e e esse id que o
        // usuario ve ao reportar uma barreira ali.
        assertThat(grafo.arestasQuePartemDe("destino")).singleElement()
                .extracting(SegmentoVia::id).isEqualTo("av-1-2");
    }

    @Test
    @DisplayName("Descer o trecho e o oposto de subi-lo")
    void arestaDeVoltaInverteOSinalDaInclinacao() {
        GrafoVias grafo = GrafoVias.de(List.of(
                calcadaBoa("rampa", ORIGEM, DESTINO).inclinacaoPercentual(7.5).construir()));

        assertThat(grafo.arestasQuePartemDe("origem").getFirst().inclinacaoPercentual()).isEqualTo(7.5);
        assertThat(grafo.arestasQuePartemDe("destino").getFirst().inclinacaoPercentual()).isEqualTo(-7.5);
    }

    @Test
    @DisplayName("Trecho de mao unica so e percorrivel no sentido cadastrado")
    void unidirecionalNaoGeraArestaDeVolta() {
        GrafoVias grafo = GrafoVias.de(
                List.of(),
                List.of(calcadaBoa("passarela", ORIGEM, DESTINO).construir()));

        assertThat(grafo.arestasQuePartemDe("origem")).hasSize(1);
        assertThat(grafo.arestasQuePartemDe("destino")).isEmpty();
    }

    @Test
    @DisplayName("O comprimento vem da geometria quando o levantamento nao informa")
    void derivaComprimentoDaDistanciaGeodesica() {
        SegmentoVia trecho = calcadaBoa("av-1-2", ORIGEM, ESQUINA_NORTE).construir();

        assertThat(trecho.comprimentoMetros())
                .isCloseTo(ORIGEM.coordenada().distanciaEmMetrosAte(ESQUINA_NORTE.coordenada()),
                        Offset.offset(0.01));
    }

    @Test
    @DisplayName("Ponto alem do raio de aproximacao nao se liga a malha")
    void snapRespeitaORaioMaximo() {
        GrafoVias grafo = GrafoVias.de(List.of(calcadaBoa("av-1-2", ORIGEM, DESTINO).construir()));
        Coordenada saoPaulo = new Coordenada(-23.5505, -46.6333);

        assertThat(grafo.noMaisProximoDe(ORIGEM.coordenada(), 50)).isPresent();
        assertThat(grafo.noMaisProximoDe(saoPaulo, 400)).isEmpty();
    }
}
