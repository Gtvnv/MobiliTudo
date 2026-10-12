package br.com.mobilitudo.obstaculos.infraestrutura.adaptador;

import br.com.mobilitudo.compartilhado.dominio.AreaRetangular;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import br.com.mobilitudo.obstaculos.dominio.porta.RepositorioObstaculos;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;
import br.com.mobilitudo.rotas.dominio.porta.FonteDeObstaculos;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adaptador que publica, para o contexto de rotas, apenas os obstaculos ja validados.
 *
 * <p>E aqui que a fronteira entre os dois contextos e mantida honesta: um relato pendente
 * ou ja resolvido nunca chega ao calculo de rota, e o agregado completo nao vaza junto.</p>
 */
@Component
class FonteDeObstaculosValidados implements FonteDeObstaculos {

    private final RepositorioObstaculos repositorio;

    FonteDeObstaculosValidados(RepositorioObstaculos repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<ObstaculoNoTrecho>> porSegmentoNaArea(AreaRetangular area) {
        return repositorio.porStatusCompleto(StatusValidacao.VALIDADO).stream()
                .filter(obstaculo -> area.contem(obstaculo.coordenada()))
                .filter(obstaculo -> obstaculo.segmentoViaId().isPresent())
                .collect(Collectors.groupingBy(
                        obstaculo -> obstaculo.segmentoViaId().orElseThrow(),
                        Collectors.mapping(FonteDeObstaculosValidados::projetar, Collectors.toList())));
    }

    private static ObstaculoNoTrecho projetar(Obstaculo obstaculo) {
        return new ObstaculoNoTrecho(obstaculo.tipo(), obstaculo.severidade());
    }
}
