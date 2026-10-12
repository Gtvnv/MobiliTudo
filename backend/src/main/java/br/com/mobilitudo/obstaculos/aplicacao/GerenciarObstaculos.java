package br.com.mobilitudo.obstaculos.aplicacao;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.PaginaDeResultado;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.ObstaculoNaoEncontradoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import br.com.mobilitudo.obstaculos.dominio.porta.RepositorioObstaculos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Casos de uso do ciclo colaborativo: reportar, conferir e dar baixa em barreiras urbanas.
 *
 * <p>As transicoes de estado ficam no agregado {@link Obstaculo}; aqui so acontecem a busca,
 * a chamada e a gravacao.</p>
 */
@Service
public class GerenciarObstaculos {

    private final RepositorioObstaculos repositorio;

    public GerenciarObstaculos(RepositorioObstaculos repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Obstaculo reportar(Comando comando) {
        Obstaculo obstaculo = Obstaculo.reportar(
                comando.tipo(),
                comando.severidade(),
                comando.coordenada(),
                comando.descricao(),
                comando.segmentoViaId(),
                comando.reportadoPor());
        return repositorio.salvar(obstaculo);
    }

    @Transactional
    public Obstaculo validar(UUID id) {
        return aplicar(id, Obstaculo::validar);
    }

    @Transactional
    public Obstaculo rejeitar(UUID id) {
        return aplicar(id, Obstaculo::rejeitar);
    }

    @Transactional
    public Obstaculo marcarResolvido(UUID id) {
        return aplicar(id, Obstaculo::marcarResolvido);
    }

    @Transactional(readOnly = true)
    public PaginaDeResultado<Obstaculo> porStatus(StatusValidacao status, Paginacao paginacao) {
        return PaginaDeResultado.buscar(paginacao, p -> repositorio.porStatus(status, p));
    }

    @Transactional(readOnly = true)
    public PaginaDeResultado<Obstaculo> naArea(Coordenada centro, double raioMetros, Paginacao paginacao) {
        return PaginaDeResultado.buscar(paginacao, p -> repositorio.naArea(centro, raioMetros, p));
    }

    private Obstaculo aplicar(UUID id, java.util.function.Consumer<Obstaculo> transicao) {
        Obstaculo obstaculo = repositorio.porId(id).orElseThrow(() -> new ObstaculoNaoEncontradoException(id));
        transicao.accept(obstaculo);
        return repositorio.salvar(obstaculo);
    }

    public record Comando(TipoObstaculo tipo,
                          Severidade severidade,
                          Coordenada coordenada,
                          String descricao,
                          String segmentoViaId,
                          String reportadoPor) {
    }
}
