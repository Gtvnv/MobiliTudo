package br.com.mobilitudo.obstaculos.dominio;

import br.com.mobilitudo.obstaculos.dominio.servico.TokenDeExclusao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenDeExclusaoTest {

    @Test
    @DisplayName("Cada token gerado é diferente do anterior")
    void geraTokensDistintos() {
        Set<String> gerados = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            gerados.add(TokenDeExclusao.gerar());
        }

        assertThat(gerados).hasSize(1000);
    }

    @Test
    @DisplayName("O token gerado é longo o bastante para não ser adivinhável por força bruta")
    void tokenTemEntropiaSuficiente() {
        String token = TokenDeExclusao.gerar();

        // 32 bytes aleatórios em Base64 sem padding rendem 43 caracteres.
        assertThat(token.length()).isGreaterThanOrEqualTo(40);
    }

    @Test
    @DisplayName("O hash é determinístico: o mesmo token sempre produz o mesmo hash")
    void hashEDeterministico() {
        String token = TokenDeExclusao.gerar();

        assertThat(TokenDeExclusao.hash(token)).isEqualTo(TokenDeExclusao.hash(token));
    }

    @Test
    @DisplayName("Tokens diferentes produzem hashes diferentes")
    void hashesDiferentesParaTokensDiferentes() {
        String tokenA = TokenDeExclusao.gerar();
        String tokenB = TokenDeExclusao.gerar();

        assertThat(TokenDeExclusao.hash(tokenA)).isNotEqualTo(TokenDeExclusao.hash(tokenB));
    }

    @Test
    @DisplayName("O hash nunca carrega o texto puro do token")
    void hashNaoExpoeOTokenEmTextoPuro() {
        String token = TokenDeExclusao.gerar();

        assertThat(TokenDeExclusao.hash(token)).doesNotContain(token);
    }

    @Test
    @DisplayName("confere aceita o token certo contra o hash armazenado")
    void confereAceitaOTokenCorreto() {
        String token = TokenDeExclusao.gerar();
        String hash = TokenDeExclusao.hash(token);

        assertThat(TokenDeExclusao.confere(token, hash)).isTrue();
    }

    @Test
    @DisplayName("confere recusa um token que não corresponde ao hash armazenado")
    void confereRecusaTokenErrado() {
        String hash = TokenDeExclusao.hash(TokenDeExclusao.gerar());

        assertThat(TokenDeExclusao.confere("um-chute-qualquer", hash)).isFalse();
    }

    @Test
    @DisplayName("confere não lança exceção com entradas nulas, só recusa")
    void confereToleraEntradasNulas() {
        String hash = TokenDeExclusao.hash(TokenDeExclusao.gerar());

        assertThat(TokenDeExclusao.confere(null, hash)).isFalse();
        assertThat(TokenDeExclusao.confere("qualquer-coisa", null)).isFalse();
        assertThat(TokenDeExclusao.confere(null, null)).isFalse();
    }
}
