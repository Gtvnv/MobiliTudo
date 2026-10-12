package br.com.mobilitudo.rotas.dominio;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.rotas.dominio.modelo.NoVia;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;
import br.com.mobilitudo.rotas.dominio.modelo.TipoSuperficie;

/**
 * Malha minima usada pelos testes de rota, com geometria de Praia Grande.
 *
 * <p>Os comprimentos nao sao informados de proposito: o dominio os deriva da distancia
 * geodesica entre os nos. Isso mantem comprimento e coordenada coerentes entre si, que
 * e a condicao para a heuristica do A* continuar admissivel nos testes.</p>
 *
 * <pre>
 *   esquina-norte ----------- nordeste
 *        |  \
 *        |   \ (~231 m)
 *   (111 m)   \
 *        |     \
 *     origem ---+------------ destino      (direto, ~203 m)
 *        |     /
 *   (111 m)   /
 *        |   / (~231 m)
 *   esquina-sul
 * </pre>
 */
final class MalhaDeTeste {

    static final NoVia ORIGEM = no("origem", -23.9960, -46.4140);
    static final NoVia ESQUINA_NORTE = no("esquina-norte", -23.9950, -46.4140);
    static final NoVia ESQUINA_SUL = no("esquina-sul", -23.9970, -46.4140);
    static final NoVia DESTINO = no("destino", -23.9960, -46.4120);
    static final NoVia NORDESTE = no("nordeste", -23.9950, -46.4120);

    private MalhaDeTeste() {
    }

    static NoVia no(String id, double latitude, double longitude) {
        return new NoVia(id, new Coordenada(latitude, longitude));
    }

    /** Calcada plana, larga, de concreto: o trecho ideal para qualquer perfil. */
    static SegmentoVia.Construtor calcadaBoa(String id, NoVia origem, NoVia destino) {
        return SegmentoVia.comId(id)
                .nomeVia("Rua " + id)
                .entre(origem, destino)
                .superficie(TipoSuperficie.CONCRETO)
                .larguraMetros(2.0)
                .inclinacaoPercentual(0)
                .possuiRebaixamentoDeGuia(true)
                .iluminacaoAdequada(true);
    }
}
