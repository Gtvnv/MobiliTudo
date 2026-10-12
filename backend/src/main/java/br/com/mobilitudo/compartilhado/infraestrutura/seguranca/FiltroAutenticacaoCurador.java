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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege a curadoria de obstáculos: validar, rejeitar ou resolver um relato.
 *
 * <p>Sem isto, qualquer pessoa que descobrisse o endpoint podia validar uma barreira
 * inventada — e fazer o cálculo de rota desviar todo mundo por um caminho pior — ou
 * resolver uma barreira real, apagando-a do mapa de quem depende dela. O relato em si
 * ({@code POST /obstaculos}) continua aberto de propósito: qualquer pessoa pode reportar;
 * só a confirmação exige confiança.</p>
 *
 * <p>É um segredo compartilhado simples, não um sistema de usuários — o projeto ainda não
 * tem um. O ponto principal não é a sofisticação do mecanismo, é que exista um: a
 * comparação é em tempo constante ({@link MessageDigest#isEqual}) para não vazar o
 * tamanho de um prefixo correto por timing, e a ausência de configuração falha fechada,
 * nunca aberta — sem {@code MOBILITUDO_CURADOR_TOKEN} definido, a curadoria fica
 * bloqueada, nunca liberada por omissão.</p>
 */
public class FiltroAutenticacaoCurador extends OncePerRequestFilter {

    public static final String CABECALHO_TOKEN = "X-Mobilitudo-Curador-Token";

    private final String tokenConfigurado;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public FiltroAutenticacaoCurador(String tokenConfigurado) {
        this.tokenConfigurado = tokenConfigurado;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        // O filtro é registrado em /api/v1/obstaculos/*, mais amplo do que a rota que
        // protege, porque o padrão de URL de servlet não aceita curinga no meio do
        // caminho (só um /* no final). Tudo que não é "POST .../validacao" passa direto:
        // listar e reportar obstáculos continuam abertos de propósito.
        if (!"POST".equalsIgnoreCase(requisicao.getMethod()) || !requisicao.getRequestURI().endsWith("/validacao")) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        if (tokenConfigurado == null || tokenConfigurado.isBlank()) {
            recusar(resposta, HttpStatus.SERVICE_UNAVAILABLE,
                    "Curadoria de obstáculos desativada: defina MOBILITUDO_CURADOR_TOKEN no servidor.");
            return;
        }

        String enviado = requisicao.getHeader(CABECALHO_TOKEN);
        if (enviado == null || !comparacaoEmTempoConstante(tokenConfigurado, enviado)) {
            recusar(resposta, HttpStatus.UNAUTHORIZED,
                    "Cabeçalho " + CABECALHO_TOKEN + " ausente ou inválido.");
            return;
        }

        cadeia.doFilter(requisicao, resposta);
    }

    /**
     * {@link MessageDigest#isEqual} compara byte a byte em tempo constante, o que evita
     * vazar por timing quantos caracteres do token o requisitante acertou. O comprimento
     * em si não é segredo — só o conteúdo é — então a saída antecipada para tamanhos
     * diferentes não enfraquece a defesa.
     */
    private boolean comparacaoEmTempoConstante(String esperado, String recebido) {
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recebido.getBytes(StandardCharsets.UTF_8));
    }

    private void recusar(HttpServletResponse resposta, HttpStatus status, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle("Acesso de curadoria recusado");
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(resposta.getWriter(), problema);
    }
}
