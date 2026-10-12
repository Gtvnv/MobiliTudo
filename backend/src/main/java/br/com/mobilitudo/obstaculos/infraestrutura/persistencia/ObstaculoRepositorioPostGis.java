package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import br.com.mobilitudo.compartilhado.infraestrutura.persistencia.PaginacaoJpa;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import br.com.mobilitudo.obstaculos.dominio.porta.RepositorioObstaculos;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de saida: obstaculos persistidos em PostgreSQL + PostGIS. */
@Repository
class ObstaculoRepositorioPostGis implements RepositorioObstaculos {

    private final ObstaculoJpaRepository jpa;

    ObstaculoRepositorioPostGis(ObstaculoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Obstaculo salvar(Obstaculo obstaculo) {
        return jpa.save(ObstaculoEntidade.de(obstaculo)).paraDominio();
    }

    @Override
    public Optional<Obstaculo> porId(UUID id) {
        return jpa.findById(id).map(ObstaculoEntidade::paraDominio);
    }

    @Override
    public List<Obstaculo> porStatus(StatusValidacao status, Paginacao paginacao) {
        return jpa.findAllByStatusOrderByReportadoEmDesc(status, PaginacaoJpa.semOrdenacao(paginacao)).stream()
                .map(ObstaculoEntidade::paraDominio)
                .toList();
    }

    @Override
    public List<Obstaculo> porStatusCompleto(StatusValidacao status) {
        return jpa.findAllByStatus(status).stream()
                .map(ObstaculoEntidade::paraDominio)
                .toList();
    }

    @Override
    public List<Obstaculo> naArea(Coordenada centro, double raioMetros, Paginacao paginacao) {
        return jpa.buscarNaArea(centro.latitude(), centro.longitude(), raioMetros,
                        PaginacaoJpa.semOrdenacao(paginacao)).stream()
                .map(ObstaculoEntidade::paraDominio)
                .toList();
    }
}
