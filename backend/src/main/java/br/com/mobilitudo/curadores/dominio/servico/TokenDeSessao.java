package br.com.mobilitudo.curadores.dominio.servico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token de sessão de login — mesmo padrão de {@code TokenDeExclusao} no contexto de
 * obstáculos (gerar e derivar hash), aplicado aqui à sessão em vez de à exclusão de dado
 * pessoal. Só o hash é persistido; o texto puro só existe na resposta do login e no
 * cabeçalho {@code Authorization} de quem está autenticado.
 *
 * <p>Sem método {@code confere()}: diferente da exclusão de obstáculo (que compara o
 * token recebido contra um único hash já em mãos), uma sessão é encontrada por busca
 * exata do hash no banco ({@code WHERE hash_token = ?}) — não há um segundo valor em
 * memória para comparar byte a byte, então a preocupação de timing que justifica
 * {@code MessageDigest.isEqual} em {@code TokenDeExclusao.confere} não se aplica aqui.</p>
 *
 * <p>Token opaco revogável (apagar a linha de {@code sessao_curador} invalida na hora),
 * não um JWT autocontido — que continuaria válido até expirar mesmo que a conta fosse
 * desativada ou a sessão encerrada no meio do caminho.</p>
 */
public final class TokenDeSessao {

    private static final SecureRandom GERADOR_ALEATORIO = new SecureRandom();
    private static final int TAMANHO_EM_BYTES = 32;

    private TokenDeSessao() {
    }

    public static String gerar() {
        byte[] bytesAleatorios = new byte[TAMANHO_EM_BYTES];
        GERADOR_ALEATORIO.nextBytes(bytesAleatorios);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios);
    }

    public static String hash(String tokenEmTextoPuro) {
        try {
            MessageDigest digestor = MessageDigest.getInstance("SHA-256");
            byte[] digest = digestor.digest(tokenEmTextoPuro.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", impossivel);
        }
    }
}
