package br.com.mobilitudo.curadores.dominio.servico;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de senha de curador: PBKDF2-HMAC-SHA256, JDK puro — sem dependência de
 * framework, mesmo raciocínio de {@code TokenDeExclusao} no contexto de obstáculos. Não
 * foi preciso trazer BCrypt (nem o Spring Security inteiro só para isso): o
 * {@code javax.crypto} já embutido na JVM é suficiente e evita mais uma dependência
 * externa num projeto que já tenta manter o mínimo possível fora do que o JDK oferece.
 *
 * <p>210.000 iterações segue a recomendação da OWASP (2023) para PBKDF2-HMAC-SHA256 — alto
 * o bastante para encarecer um ataque de força bruta offline, sem pesar na latência de um
 * login humano (a verificação continua na casa de dezenas de milissegundos).</p>
 *
 * <p>O hash guardado carrega o próprio salt e a contagem de iterações, no formato
 * {@code iteracoes:saltBase64:hashBase64} — assim a contagem pode subir no futuro sem
 * invalidar hashes antigos nem exigir uma coluna extra na tabela.</p>
 */
public final class HashDeSenha {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_DA_CHAVE_EM_BITS = 256;
    private static final int TAMANHO_DO_SALT_EM_BYTES = 16;

    private static final SecureRandom GERADOR_ALEATORIO = new SecureRandom();

    private HashDeSenha() {
    }

    public static String hash(String senhaEmTextoPuro) {
        byte[] salt = new byte[TAMANHO_DO_SALT_EM_BYTES];
        GERADOR_ALEATORIO.nextBytes(salt);
        byte[] derivada = derivar(senhaEmTextoPuro, salt, ITERACOES);

        return ITERACOES + ":" + codificar(salt) + ":" + codificar(derivada);
    }

    /**
     * Confere a senha em tempo essencialmente constante: a comparação final usa
     * {@link MessageDigest#isEqual}, o mesmo raciocínio de {@code TokenDeExclusao} e do
     * antigo token de curadoria — não vazar por timing quanto da senha certa foi
     * acertado. Um hash em formato inválido (corrompido, ou de uma versão futura deste
     * método que este código não reconhece) é tratado como "não confere", nunca como
     * erro que derruba a requisição.
     */
    public static boolean confere(String senhaEmTextoPuro, String hashArmazenado) {
        try {
            String[] partes = hashArmazenado.split(":", 3);
            int iteracoes = Integer.parseInt(partes[0]);
            byte[] salt = decodificar(partes[1]);
            byte[] hashEsperado = decodificar(partes[2]);

            byte[] hashCalculado = derivar(senhaEmTextoPuro, salt, iteracoes);
            return MessageDigest.isEqual(hashCalculado, hashEsperado);
        } catch (RuntimeException formatoInesperado) {
            return false;
        }
    }

    private static byte[] derivar(String senhaEmTextoPuro, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec especificacao = new PBEKeySpec(
                    senhaEmTextoPuro.toCharArray(), salt, iteracoes, TAMANHO_DA_CHAVE_EM_BITS);
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            return fabrica.generateSecret(especificacao).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException impossivel) {
            // PBKDF2WithHmacSHA256 é obrigatório em toda implementação de JDK desde o
            // Java 8; isto nunca deveria disparar fora de um ambiente Java quebrado.
            throw new IllegalStateException("PBKDF2WithHmacSHA256 indisponível na JVM", impossivel);
        }
    }

    private static String codificar(byte[] bytes) {
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decodificar(String texto) {
        return Base64.getDecoder().decode(texto);
    }
}
