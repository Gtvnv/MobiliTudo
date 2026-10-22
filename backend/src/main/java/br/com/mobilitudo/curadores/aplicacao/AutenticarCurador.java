package br.com.mobilitudo.curadores.aplicacao;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import br.com.mobilitudo.curadores.dominio.modelo.CredenciaisInvalidasException;
import br.com.mobilitudo.curadores.dominio.modelo.SessaoDeCurador;
import br.com.mobilitudo.curadores.dominio.porta.RepositorioCuradores;
import br.com.mobilitudo.curadores.dominio.porta.RepositorioSessoesCurador;
import br.com.mobilitudo.curadores.dominio.servico.TokenDeSessao;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Login, logout e verificação de sessão — a fronteira de autenticação que substitui o
 * antigo {@code MOBILITUDO_CURADOR_TOKEN} compartilhado.
 */
@Service
public class AutenticarCurador {

    private final RepositorioCuradores repositorioCuradores;
    private final RepositorioSessoesCurador repositorioSessoes;
    private final LimitadorDeTentativasDeLogin limitadorDeTentativas;
    private final Duration duracaoDaSessao;

    public AutenticarCurador(RepositorioCuradores repositorioCuradores,
                             RepositorioSessoesCurador repositorioSessoes,
                             LimitadorDeTentativasDeLogin limitadorDeTentativas,
                             @Value("${mobilitudo.seguranca.sessao-curador-dias:30}") long duracaoDaSessaoEmDias) {
        this.repositorioCuradores = repositorioCuradores;
        this.repositorioSessoes = repositorioSessoes;
        this.limitadorDeTentativas = limitadorDeTentativas;
        this.duracaoDaSessao = Duration.ofDays(duracaoDaSessaoEmDias);
    }

    /**
     * @return o token de sessão em texto puro — só existe aqui; o que é persistido é só
     *         o hash
     * @throws CredenciaisInvalidasException e-mail não cadastrado, senha errada, ou
     *                                       conta desativada — as três respondem igual,
     *                                       de propósito (ver a exceção)
     * @throws LimiteDeTentativasDeLoginExcedidoException limite de tentativas de login
     *                                       da origem esgotado — ver
     *                                       {@link LimitadorDeTentativasDeLogin}
     */
    @Transactional
    public ResultadoDeLogin login(String email, String senha, HttpServletRequest requisicao) {
        limitadorDeTentativas.exigirDentroDoLimite(requisicao);

        Curador curador = repositorioCuradores.porEmail(email == null ? "" : email)
                .filter(c -> c.autenticaCom(senha))
                .orElseThrow(CredenciaisInvalidasException::new);

        String tokenEmTextoPuro = TokenDeSessao.gerar();
        Instant agora = Instant.now();
        SessaoDeCurador sessao = new SessaoDeCurador(UUID.randomUUID(), curador.id(), agora, agora.plus(duracaoDaSessao));
        repositorioSessoes.salvar(sessao, TokenDeSessao.hash(tokenEmTextoPuro));

        return new ResultadoDeLogin(curador, tokenEmTextoPuro, sessao.expiraEm());
    }

    @Transactional
    public void logout(String tokenEmTextoPuro) {
        repositorioSessoes.porHashToken(TokenDeSessao.hash(tokenEmTextoPuro))
                .ifPresent(sessao -> repositorioSessoes.remover(sessao.id()));
    }

    /**
     * Resolve o token de sessão apresentado para o curador correspondente — a
     * verificação autoritativa, chamada direto do controller, pelo mesmo raciocínio
     * documentado em {@code AutenticadorDeCurador} do contexto de obstáculos (que agora
     * delega para cá): token ausente, inválido, expirado, ou de uma conta desativada
     * respondem todos com a mesma exceção.
     */
    @Transactional(readOnly = true)
    public Curador autenticarPorToken(String tokenEmTextoPuro) {
        if (tokenEmTextoPuro == null || tokenEmTextoPuro.isBlank()) {
            throw new CredenciaisInvalidasException();
        }
        SessaoDeCurador sessao = repositorioSessoes.porHashToken(TokenDeSessao.hash(tokenEmTextoPuro))
                .orElseThrow(CredenciaisInvalidasException::new);
        if (sessao.expirada(Instant.now())) {
            repositorioSessoes.remover(sessao.id());
            throw new CredenciaisInvalidasException();
        }
        Curador curador = repositorioCuradores.porId(sessao.curadorId())
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!curador.ativo()) {
            throw new CredenciaisInvalidasException();
        }
        return curador;
    }

    public record ResultadoDeLogin(Curador curador, String tokenEmTextoPuro, Instant expiraEm) {
    }
}
