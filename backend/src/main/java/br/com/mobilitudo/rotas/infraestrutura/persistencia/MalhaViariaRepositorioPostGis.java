package br.com.mobilitudo.rotas.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.dominio.AreaRetangular;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.rotas.dominio.modelo.GrafoVias;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;
import br.com.mobilitudo.rotas.dominio.porta.FonteDeObstaculos;
import br.com.mobilitudo.rotas.dominio.porta.RepositorioMalhaViaria;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de saida que monta o grafo de vias a partir do PostGIS.
 *
 * <p>Faz duas coisas que o dominio nao deve fazer: recorta a malha espacialmente e junta
 * os obstaculos validados de outro contexto aos trechos. O que sai daqui e um
 * {@link GrafoVias} puro, pronto para o A* trabalhar.</p>
 */
@Repository
class MalhaViariaRepositorioPostGis implements RepositorioMalhaViaria {

    private final SegmentoViaJpaRepository jpa;
    private final FonteDeObstaculos fonteDeObstaculos;

    MalhaViariaRepositorioPostGis(SegmentoViaJpaRepository jpa, FonteDeObstaculos fonteDeObstaculos) {
        this.jpa = jpa;
        this.fonteDeObstaculos = fonteDeObstaculos;
    }

    @Override
    @Transactional(readOnly = true)
    public GrafoVias carregarNaAreaEntre(Coordenada origem, Coordenada destino, double margemMetros) {
        AreaRetangular area = AreaRetangular.envolvendo(origem, destino, margemMetros);

        List<SegmentoViaEntidade> entidades = jpa.buscarNaArea(
                area.sudoeste().latitude(), area.sudoeste().longitude(),
                area.nordeste().latitude(), area.nordeste().longitude());

        Map<String, List<ObstaculoNoTrecho>> obstaculosPorSegmento =
                fonteDeObstaculos.porSegmentoNaArea(area);

        List<SegmentoVia> bidirecionais = new ArrayList<>();
        List<SegmentoVia> unidirecionais = new ArrayList<>();
        for (SegmentoViaEntidade entidade : entidades) {
            SegmentoVia segmento = entidade.paraDominio(
                    obstaculosPorSegmento.getOrDefault(entidade.id(), List.of()));
            (entidade.bidirecional() ? bidirecionais : unidirecionais).add(segmento);
        }
        return GrafoVias.de(bidirecionais, unidirecionais);
    }
}
