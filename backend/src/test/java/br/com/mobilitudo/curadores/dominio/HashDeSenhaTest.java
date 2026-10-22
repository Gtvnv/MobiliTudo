package br.com.mobilitudo.curadores.dominio;

import br.com.mobilitudo.curadores.dominio.servico.HashDeSenha;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashDeSenhaTest {

    @Test
    @DisplayName("confere aceita a senha certa contra o hash gerado")
    void confereAceitaASenhaCorreta() {
        String hash = HashDeSenha.hash("senha-bem-forte-123");

        assertThat(HashDeSenha.confere("senha-bem-forte-123", hash)).isTrue();
    }

    @Test
    @DisplayName("confere recusa uma senha errada")
    void confereRecusaSenhaErrada() {
        String hash = HashDeSenha.hash("senha-bem-forte-123");

        assertThat(HashDeSenha.confere("chute-errado", hash)).isFalse();
    }

    @Test
    @DisplayName("O hash nunca carrega a senha em texto puro")
    void hashNaoExpoeASenhaEmTextoPuro() {
        String hash = HashDeSenha.hash("senha-bem-forte-123");

        assertThat(hash).doesNotContain("senha-bem-forte-123");
    }

    @Test
    @DisplayName("Hashes da mesma senha são diferentes entre si, por causa do salt aleatório")
    void hashesDaMesmaSenhaSaoDiferentes() {
        String hashA = HashDeSenha.hash("senha-bem-forte-123");
        String hashB = HashDeSenha.hash("senha-bem-forte-123");

        assertThat(hashA).isNotEqualTo(hashB);
        assertThat(HashDeSenha.confere("senha-bem-forte-123", hashA)).isTrue();
        assertThat(HashDeSenha.confere("senha-bem-forte-123", hashB)).isTrue();
    }

    @Test
    @DisplayName("confere não lança exceção com um hash malformado, só recusa")
    void confereToleraHashMalformado() {
        assertThat(HashDeSenha.confere("qualquer-coisa", "isso-nao-e-um-hash-valido")).isFalse();
    }
}
