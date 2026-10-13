package br.com.mobilitudo.compartilhado.infraestrutura.web;

import br.com.mobilitudo.compartilhado.infraestrutura.seguranca.CuradoriaIndisponivelException;
import br.com.mobilitudo.compartilhado.infraestrutura.seguranca.LimiteDeRelatosExcedidoException;
import br.com.mobilitudo.compartilhado.infraestrutura.seguranca.TokenDeCuradorInvalidoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.ObstaculoNaoEncontradoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.TokenDeExclusaoInvalidoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.TransicaoInvalidaException;
import br.com.mobilitudo.rotas.dominio.modelo.RotaNaoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Traducao das excecoes de dominio para respostas HTTP.
 *
 * <p>Concentrar isso aqui e o que permite ao dominio lancar excecoes com significado de
 * negocio, sem nunca mencionar um status code.</p>
 */
@RestControllerAdvice
class ManipuladorGlobalDeErros {

    private static final Logger LOG = LoggerFactory.getLogger(ManipuladorGlobalDeErros.class);

    /**
     * Rota inexistente responde 404, mas com o motivo no corpo.
     *
     * <p>"Fora da area mapeada" e "sem caminho acessivel" sao coisas diferentes para quem
     * perguntou, e a segunda e um dado valioso: e uma barreira urbana que isola um destino.</p>
     */
    @ExceptionHandler(RotaNaoEncontradaException.class)
    ProblemDetail rotaNaoEncontrada(RotaNaoEncontradaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Rota não encontrada");
        problema.setProperty("motivo", excecao.motivo().name());
        return problema;
    }

    @ExceptionHandler(ObstaculoNaoEncontradoException.class)
    ProblemDetail obstaculoNaoEncontrado(ObstaculoNaoEncontradoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Obstáculo não encontrado");
        return problema;
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    ProblemDetail transicaoInvalida(TransicaoInvalidaException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Transição de status não permitida");
        return problema;
    }

    /**
     * 403, não 401: não há esquema de autenticação sendo desafiado aqui, é posse de um
     * segredo específico daquele relato — mais próximo de "você não tem permissão para
     * esta ação neste recurso" do que de "autentique-se".
     */
    @ExceptionHandler(TokenDeExclusaoInvalidoException.class)
    ProblemDetail tokenDeExclusaoInvalido(TokenDeExclusaoInvalidoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, excecao.getMessage());
        problema.setTitle("Token de exclusão inválido");
        return problema;
    }

    @ExceptionHandler(TokenDeCuradorInvalidoException.class)
    ProblemDetail tokenDeCuradorInvalido(TokenDeCuradorInvalidoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, excecao.getMessage());
        problema.setTitle("Acesso de curadoria recusado");
        return problema;
    }

    @ExceptionHandler(CuradoriaIndisponivelException.class)
    ProblemDetail curadoriaIndisponivel(CuradoriaIndisponivelException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, excecao.getMessage());
        problema.setTitle("Acesso de curadoria recusado");
        return problema;
    }

    @ExceptionHandler(LimiteDeRelatosExcedidoException.class)
    ProblemDetail limiteDeRelatosExcedido(LimiteDeRelatosExcedidoException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, excecao.getMessage());
        problema.setTitle("Muitos relatos em pouco tempo");
        return problema;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
        problema.setTitle("Requisição inválida");
        return problema;
    }

    /**
     * Parâmetro de caminho ou de query no formato errado (id que não é UUID, "raio" que
     * não é número, etc.). É erro do cliente, não bug do servidor — por isso tem handler
     * próprio em vez de cair no genérico de {@link #erroInesperado}, que responde 500. A
     * mensagem é construída a partir do nome do parâmetro, não do texto bruto da
     * exceção, que incluiria o nome completo da classe interna esperada
     * ({@code java.util.UUID}, por exemplo) sem necessidade.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroComFormatoInvalido(MethodArgumentTypeMismatchException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Parâmetro '" + excecao.getName() + "' tem formato inválido.");
        problema.setTitle("Requisição inválida");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacaoDeEntrada(MethodArgumentNotValidException excecao) {
        String detalhes = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhes);
        problema.setTitle("Requisição inválida");
        return problema;
    }

    /**
     * Rede de segurança para qualquer exceção que nenhum handler acima reconheceu —
     * um bug real, uma falha de infraestrutura (banco fora do ar, etc.), qualquer coisa
     * não antecipada. A mensagem da exceção fica só no log do servidor; quem chamou a
     * API recebe um 500 genérico, nunca o texto bruto de uma exceção que ninguém decidiu
     * de propósito que fosse seguro expor. Isso é o que efetivamente impede uma falha
     * inesperada de virar um vazamento de informação — bem mais confiável do que contar
     * com o comportamento padrão do Spring para cada tipo de exceção possível.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail erroInesperado(Exception excecao) {
        LOG.error("Erro não tratado ao processar requisição", excecao);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente; se persistir, é um bug.");
        problema.setTitle("Erro interno");
        return problema;
    }
}
