package br.com.mobilitudo.curadores.dominio;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import br.com.mobilitudo.curadores.dominio.modelo.SenhaFracaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuradorTest {

    @Test
    @DisplayName("Conta nova nasce ativa e autentica com a senha com que foi criada")
    void contaNovaAutenticaComASenhaCorreta() {
        Curador curador = Curador.criar("Curadora@Mobilitudo.org", "Yasmim", "senha-forte-123");

        assertThat(curador.ativo()).isTrue();
        assertThat(curador.autenticaCom("senha-forte-123")).isTrue();
    }

    @Test
    @DisplayName("E-mail é normalizado para minúsculas e sem espaços nas pontas")
    void emailENormalizado() {
        Curador curador = Curador.criar("  Curadora@Mobilitudo.org  ", "Yasmim", "senha-forte-123");

        assertThat(curador.email()).isEqualTo("curadora@mobilitudo.org");
    }

    @Test
    @DisplayName("Senha errada não autentica")
    void senhaErradaNaoAutentica() {
        Curador curador = Curador.criar("curadora@mobilitudo.org", "Yasmim", "senha-forte-123");

        assertThat(curador.autenticaCom("chute-errado")).isFalse();
    }

    @Test
    @DisplayName("Conta desativada não autentica mesmo com a senha certa")
    void contaDesativadaNaoAutenticaComASenhaCerta() {
        Curador curador = Curador.criar("curadora@mobilitudo.org", "Yasmim", "senha-forte-123");

        curador.desativar();

        assertThat(curador.ativo()).isFalse();
        assertThat(curador.autenticaCom("senha-forte-123")).isFalse();
    }

    @Test
    @DisplayName("Senha com menos de 8 caracteres é recusada na criação")
    void senhaCurtaERecusada() {
        assertThatThrownBy(() -> Curador.criar("curadora@mobilitudo.org", "Yasmim", "curta"))
                .isInstanceOf(SenhaFracaException.class);
    }

    @Test
    @DisplayName("Senha nula é recusada na criação")
    void senhaNulaERecusada() {
        assertThatThrownBy(() -> Curador.criar("curadora@mobilitudo.org", "Yasmim", null))
                .isInstanceOf(SenhaFracaException.class);
    }

    @Test
    @DisplayName("O hash da senha nunca contém a senha em texto puro")
    void hashSenhaNaoExpoeATextoPuro() {
        Curador curador = Curador.criar("curadora@mobilitudo.org", "Yasmim", "senha-forte-123");

        assertThat(curador.hashSenha()).doesNotContain("senha-forte-123");
    }
}
