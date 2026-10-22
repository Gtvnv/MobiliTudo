package br.com.mobilitudo.curadores.dominio;

import br.com.mobilitudo.curadores.dominio.servico.TokenDeSessao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenDeSessaoTest {

    @Test
    @DisplayName("Cada token gerado é diferente do anterior")
    void geraTokensDistintos() {
        Set<String> gerados = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            gerados.add(TokenDeSessao.gerar());
        }

        assertThat(gerados).hasSize(1000);
    }

    @Test
    @DisplayName("O token gerado é longo o bastante para não ser adivinhável por força bruta")
    void tokenTemEntropiaSuficiente() {
        String token = TokenDeSessao.gerar();

        assertThat(token.length()).isGreaterThanOrEqualTo(40);
    }

    @Test
    @DisplayName("O hash é determinístico: o mesmo token sempre produz o mesmo hash")
    void hashEDeterministico() {
        String token = TokenDeSessao.gerar();

        assertThat(TokenDeSessao.hash(token)).isEqualTo(TokenDeSessao.hash(token));
    }

    @Test
    @DisplayName("Tokens diferentes produzem hashes diferentes")
    void hashesDiferentesParaTokensDiferentes() {
        String tokenA = TokenDeSessao.gerar();
        String tokenB = TokenDeSessao.gerar();

        assertThat(TokenDeSessao.hash(tokenA)).isNotEqualTo(TokenDeSessao.hash(tokenB));
    }

    @Test
    @DisplayName("O hash nunca carrega o texto puro do token")
    void hashNaoExpoeOTokenEmTextoPuro() {
        String token = TokenDeSessao.gerar();

        assertThat(TokenDeSessao.hash(token)).doesNotContain(token);
    }
}
