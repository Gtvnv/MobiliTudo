package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Testa o componente que agora é a fronteira de verdade da curadoria — chamado direto
 * do controller, não mais por um {@code Filter} decidindo com base em texto bruto de
 * URL. Não depende de {@code FilterChain} nem de mock de servlet além do request:
 * {@link AutenticadorDeCurador} não sabe nada sobre filtros, só sobre o cabeçalho.
 */
class AutenticadorDeCuradorTest {

    @Test
    @DisplayName("Sem token configurado no servidor, falha fechado mesmo sem cabeçalho algum")
    void semTokenConfiguradoFalhaFechado() {
        var autenticador = new AutenticadorDeCurador("");
        var requisicao = new MockHttpServletRequest();

        assertThatThrownBy(() -> autenticador.exigirTokenValido(requisicao))
                .isInstanceOf(CuradoriaIndisponivelException.class);
    }

    @Test
    @DisplayName("Requisição sem o cabeçalho de token é recusada")
    void semCabecalhoERecusado() {
        var autenticador = new AutenticadorDeCurador("segredo-do-curador");
        var requisicao = new MockHttpServletRequest();

        assertThatThrownBy(() -> autenticador.exigirTokenValido(requisicao))
                .isInstanceOf(TokenDeCuradorInvalidoException.class);
    }

    @Test
    @DisplayName("Token incorreto é recusado")
    void tokenIncorretoERecusado() {
        var autenticador = new AutenticadorDeCurador("segredo-do-curador");
        var requisicao = new MockHttpServletRequest();
        requisicao.addHeader(AutenticadorDeCurador.CABECALHO_TOKEN, "chute-errado");

        assertThatThrownBy(() -> autenticador.exigirTokenValido(requisicao))
                .isInstanceOf(TokenDeCuradorInvalidoException.class);
    }

    @Test
    @DisplayName("Token correto não lança nada")
    void tokenCorretoPassa() {
        var autenticador = new AutenticadorDeCurador("segredo-do-curador");
        var requisicao = new MockHttpServletRequest();
        requisicao.addHeader(AutenticadorDeCurador.CABECALHO_TOKEN, "segredo-do-curador");

        assertThatNoException().isThrownBy(() -> autenticador.exigirTokenValido(requisicao));
    }
}
