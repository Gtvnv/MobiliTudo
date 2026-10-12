package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Limita quantos relatos de obstáculo uma mesma origem pode enviar por hora.
 *
 * <p>Protege só {@code POST /api/v1/obstaculos} (o relato em si). Listar continua sem
 * limite — é leitura — e a curadoria já exige token, então não precisa deste limite por
 * cima.</p>
 *
 * <p>A chave é {@link HttpServletRequest#getRemoteAddr()}, o endereço TCP de quem conectou
 * direto. Atrás de um proxy reverso que não preserve ou declare o IP original, isso conta
 * o proxy, não o cliente — correto para a topologia atual (API exposta direto); se um
 * proxy confiável entrar no caminho, a chave precisa passar a ler
 * {@code X-Forwarded-For} apenas dele, nunca de qualquer requisição, ou o próprio limite
 * vira trivialmente contornável.</p>
 */
public class FiltroLimiteDeObstaculos extends OncePerRequestFilter {

    private final LimitadorDeTaxa limitador;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public FiltroLimiteDeObstaculos(LimitadorDeTaxa limitador) {
        this.limitador = limitador;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        boolean eRelatoDeObstaculo = "POST".equalsIgnoreCase(requisicao.getMethod())
                && "/api/v1/obstaculos".equals(requisicao.getRequestURI());

        if (!eRelatoDeObstaculo || limitador.permitir(requisicao.getRemoteAddr())) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Limite de relatos de obstáculo por hora atingido para esta origem. Tente novamente mais tarde.");
        problema.setTitle("Muitos relatos em pouco tempo");
        resposta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(resposta.getWriter(), problema);
    }
}
