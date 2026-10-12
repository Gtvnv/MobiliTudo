package br.com.mobilitudo.compartilhado.infraestrutura.web;

import br.com.mobilitudo.obstaculos.dominio.modelo.ObstaculoNaoEncontradoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.TokenDeExclusaoInvalidoException;
import br.com.mobilitudo.obstaculos.dominio.modelo.TransicaoInvalidaException;
import br.com.mobilitudo.rotas.dominio.modelo.RotaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traducao das excecoes de dominio para respostas HTTP.
 *
 * <p>Concentrar isso aqui e o que permite ao dominio lancar excecoes com significado de
 * negocio, sem nunca mencionar um status code.</p>
 */
@RestControllerAdvice
class ManipuladorGlobalDeErros {

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

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail argumentoInvalido(IllegalArgumentException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
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
}
