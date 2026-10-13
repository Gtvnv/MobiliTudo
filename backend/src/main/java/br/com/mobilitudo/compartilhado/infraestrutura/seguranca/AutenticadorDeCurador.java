package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 * tem um. A comparação é em tempo constante ({@link MessageDigest#isEqual}) para não
 * vazar o tamanho de um prefixo correto por timing, e a ausência de configuração falha
 * fechada, nunca aberta — sem {@code MOBILITUDO_CURADOR_TOKEN} definido, a curadoria fica
 * bloqueada, nunca liberada por omissão.</p>
 *
 * <p><strong>Por que isto é chamado de dentro do controller, e não de um
 * {@code Filter}</strong>: um filtro decidiria se aplica a verificação comparando
 * {@code getRequestURI()} (o texto bruto da URL) contra um padrão como
 * {@code endsWith("/validacao")}. Essa é exatamente a classe de bug por trás de vários
 * desvios de autorização já documentados em frameworks web — o filtro enxerga uma string
 * de URL; o Spring MVC enxerga outra coisa ao decidir qual {@code @PostMapping} invocar
 * (parâmetros de matriz como {@code ;foo=bar}, barra dupla, normalização de codificação),
 * e as duas visões não têm garantia de concordar sempre. Chamar isto de dentro do método
 * do controller elimina essa ambiguidade por construção: se este código está executando,
 * o Spring MVC já decidiu, pelo próprio mecanismo de roteamento, que a requisição casa
 * com {@code POST .../validacao} — não sobra espaço para uma segunda opinião
 * divergente.</p>
 */
@Component
public class AutenticadorDeCurador {

    public static final String CABECALHO_TOKEN = "X-Mobilitudo-Curador-Token";

    private final String tokenConfigurado;

    public AutenticadorDeCurador(@Value("${mobilitudo.seguranca.token-curador:}") String tokenConfigurado) {
        this.tokenConfigurado = tokenConfigurado;
    }

    /**
     * @throws CuradoriaIndisponivelException se {@code MOBILITUDO_CURADOR_TOKEN} não
     *                                        estiver configurado no servidor
     * @throws TokenDeCuradorInvalidoException se o cabeçalho estiver ausente ou errado
     */
    public void exigirTokenValido(HttpServletRequest requisicao) {
        if (tokenConfigurado == null || tokenConfigurado.isBlank()) {
            throw new CuradoriaIndisponivelException();
        }
        String enviado = requisicao.getHeader(CABECALHO_TOKEN);
        if (enviado == null || !comparacaoEmTempoConstante(tokenConfigurado, enviado)) {
            throw new TokenDeCuradorInvalidoException();
        }
    }

    private boolean comparacaoEmTempoConstante(String esperado, String recebido) {
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recebido.getBytes(StandardCharsets.UTF_8));
    }
}
