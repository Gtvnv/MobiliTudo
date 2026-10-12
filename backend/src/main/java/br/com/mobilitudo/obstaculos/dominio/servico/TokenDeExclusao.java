package br.com.mobilitudo.obstaculos.dominio.servico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Prova de posse usada para a autoexclusão de dado pessoal (LGPD art. 18): quem relata
 * um obstáculo recebe este token uma única vez, na resposta da criação, e pode usá-lo
 * depois para remover o próprio nome do relato — sem precisar de conta nem depender de
 * um curador.
 *
 * <p>Só o {@link #hash} é persistido; o token em texto puro nunca é salvo em lugar
 * algum depois de gerado e devolvido na resposta. É o mesmo raciocínio de nunca guardar
 * senha em texto puro, aplicado a um segredo de posse em vez de autenticação de
 * usuário.</p>
 *
 * <p>Classe sem estado e sem dependência de framework: gerar, derivar o hash e conferir
 * são operações puras, testáveis sem Spring e sem banco.</p>
 */
public final class TokenDeExclusao {

    private static final SecureRandom GERADOR_ALEATORIO = new SecureRandom();
    private static final int TAMANHO_EM_BYTES = 32;

    private TokenDeExclusao() {
    }

    /** Gera um token de 32 bytes aleatórios, codificado em Base64 URL-safe sem padding. */
    public static String gerar() {
        byte[] bytesAleatorios = new byte[TAMANHO_EM_BYTES];
        GERADOR_ALEATORIO.nextBytes(bytesAleatorios);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios);
    }

    /** SHA-256 do token, em hexadecimal — o único valor que chega a ser persistido. */
    public static String hash(String tokenEmTextoPuro) {
        try {
            MessageDigest digestor = MessageDigest.getInstance("SHA-256");
            byte[] digest = digestor.digest(tokenEmTextoPuro.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossivel) {
            // SHA-256 é obrigatório em toda implementação de JDK desde sempre; isto nunca
            // deveria disparar fora de um ambiente Java quebrado.
            throw new IllegalStateException("SHA-256 indisponível na JVM", impossivel);
        }
    }

    /**
     * Confere um token em texto puro contra o hash armazenado, em tempo constante.
     *
     * <p>{@link MessageDigest#isEqual} evita vazar por timing quantos caracteres do
     * hash calculado coincidem com o armazenado — o mesmo raciocínio usado para o token
     * de curadoria em {@code FiltroAutenticacaoCurador}.</p>
     */
    public static boolean confere(String tokenEmTextoPuro, String hashArmazenado) {
        if (tokenEmTextoPuro == null || hashArmazenado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(tokenEmTextoPuro).getBytes(StandardCharsets.UTF_8),
                hashArmazenado.getBytes(StandardCharsets.UTF_8));
    }
}
