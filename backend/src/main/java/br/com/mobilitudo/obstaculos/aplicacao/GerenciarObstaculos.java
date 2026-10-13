package br.com.mobilitudo.obstaculos.aplicacao;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.PaginaDeResultado;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.ObstaculoNaoEncontradoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import br.com.mobilitudo.obstaculos.dominio.modelo.TokenDeExclusaoInvalidoException;
import br.com.mobilitudo.obstaculos.dominio.porta.RepositorioObstaculos;
import br.com.mobilitudo.obstaculos.dominio.porta.RepositorioTokensDeExclusao;
import br.com.mobilitudo.obstaculos.dominio.servico.TokenDeExclusao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Casos de uso do ciclo colaborativo: reportar, conferir, dar baixa em barreiras
 * urbanas, e a autoexclusão de dado pessoal de quem relatou.
 *
 * <p>As transicoes de estado ficam no agregado {@link Obstaculo}; aqui so acontecem a busca,
 * a chamada e a gravacao.</p>
 */
@Service
public class GerenciarObstaculos {

    private final RepositorioObstaculos repositorio;
    private final RepositorioTokensDeExclusao repositorioTokens;

    public GerenciarObstaculos(RepositorioObstaculos repositorio, RepositorioTokensDeExclusao repositorioTokens) {
        this.repositorio = repositorio;
        this.repositorioTokens = repositorioTokens;
    }

    /**
     * Registra o relato e gera o token de exclusão da pessoa que relatou.
     *
     * <p>O token em texto puro só existe neste retorno: só o hash dele é persistido
     * (ver {@link TokenDeExclusao}), então perder esta resposta é perder a única chance
     * de usá-lo — não há como recuperá-lo depois.</p>
     */
    @Transactional
    public ObstaculoReportado reportar(Comando comando) {
        Obstaculo obstaculo = Obstaculo.reportar(
                comando.tipo(),
                comando.severidade(),
                comando.coordenada(),
                comando.descricao(),
                comando.segmentoViaId(),
                comando.reportadoPor());
        Obstaculo salvo = repositorio.salvar(obstaculo);

        String tokenEmTextoPuro = TokenDeExclusao.gerar();
        repositorioTokens.salvar(salvo.id(), TokenDeExclusao.hash(tokenEmTextoPuro));

        return new ObstaculoReportado(salvo, tokenEmTextoPuro);
    }

    /**
     * Remove o nome de quem relatou, mediante posse do token de exclusão.
     *
     * <p>Token errado, já usado, ou relato sem token (semeado direto no banco, por
     * exemplo) respondem com a mesma exceção de propósito — ver
     * {@link TokenDeExclusaoInvalidoException}. O token é consumido após o uso: não dá
     * para anonimizar o mesmo relato duas vezes com o mesmo token, o que limita o dano
     * de um token que vaze depois de já ter sido usado pelo dono legítimo.</p>
     */
    @Transactional
    public Obstaculo anonimizar(UUID id, String tokenEmTextoPuro) {
        String hashArmazenado = repositorioTokens.hashDe(id)
                .orElseThrow(TokenDeExclusaoInvalidoException::new);
        if (!TokenDeExclusao.confere(tokenEmTextoPuro, hashArmazenado)) {
            throw new TokenDeExclusaoInvalidoException();
        }

        // porId simples, não porIdParaAtualizar: anonimizar é idempotente (já é
        // "anônimo" ou passa a ser), então duas chamadas concorrentes não têm o
        // problema de última-escrita-vence-em-silêncio que justifica a trava em
        // aplicar() — travar aqui só adicionaria contenção sem ganho de correção.
        Obstaculo obstaculo = repositorio.porId(id).orElseThrow(() -> new ObstaculoNaoEncontradoException(id));
        obstaculo.anonimizar();
        Obstaculo salvo = repositorio.salvar(obstaculo);

        repositorioTokens.remover(id);
        return salvo;
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

    /**
     * {@code porIdParaAtualizar}, não {@code porId}: uma transição de status precisa
     * travar a linha pelo resto da transação, ou duas requisições simultâneas sobre o
     * mesmo obstáculo podem cada uma ler PENDENTE, cada uma passar a verificação de
     * transição em memória, e a segunda a commitar sobrescrever a primeira em silêncio —
     * ver o porquê completo em {@code RepositorioObstaculos.porIdParaAtualizar}.
     */
    private Obstaculo aplicar(UUID id, java.util.function.Consumer<Obstaculo> transicao) {
        Obstaculo obstaculo = repositorio.porIdParaAtualizar(id)
                .orElseThrow(() -> new ObstaculoNaoEncontradoException(id));
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

    /**
     * Resultado de {@link #reportar}: o relato salvo, mais o token de exclusão em
     * texto puro — a única vez que ele existe fora do hash persistido.
     */
    public record ObstaculoReportado(Obstaculo obstaculo, String tokenExclusao) {
    }
}
