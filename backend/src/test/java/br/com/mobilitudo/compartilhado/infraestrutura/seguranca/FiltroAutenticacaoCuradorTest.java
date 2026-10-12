package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o filtro isoladamente, sem subir contexto Spring nem banco: é um
 * {@code OncePerRequestFilter} comum, então um {@link MockHttpServletRequest} e um
 * {@link MockFilterChain} bastam.
 */
class FiltroAutenticacaoCuradorTest {

    private static final String ROTA_PROTEGIDA = "/api/v1/obstaculos/11111111-1111-1111-1111-111111111111/validacao";

    @Test
    @DisplayName("Sem token configurado, a curadoria fica bloqueada mesmo sem cabeçalho algum")
    void semTokenConfiguradoFalhaFechado() throws Exception {
        var resultado = executar(new FiltroAutenticacaoCurador(""), "POST", ROTA_PROTEGIDA, null);

        assertThat(resultado.resposta.getStatus()).isEqualTo(503);
        assertThat(resultado.cadeiaChamada).isFalse();
    }

    @Test
    @DisplayName("Requisição sem o cabeçalho de token é recusada")
    void semCabecalhoERecusado() throws Exception {
        var resultado = executar(new FiltroAutenticacaoCurador("segredo-do-curador"), "POST", ROTA_PROTEGIDA, null);

        assertThat(resultado.resposta.getStatus()).isEqualTo(401);
        assertThat(resultado.cadeiaChamada).isFalse();
    }

    @Test
    @DisplayName("Token incorreto é recusado")
    void tokenIncorretoERecusado() throws Exception {
        var resultado = executar(new FiltroAutenticacaoCurador("segredo-do-curador"), "POST", ROTA_PROTEGIDA, "chute-errado");

        assertThat(resultado.resposta.getStatus()).isEqualTo(401);
        assertThat(resultado.cadeiaChamada).isFalse();
    }

    @Test
    @DisplayName("Token correto deixa a requisição passar")
    void tokenCorretoPassa() throws Exception {
        var resultado = executar(new FiltroAutenticacaoCurador("segredo-do-curador"), "POST", ROTA_PROTEGIDA, "segredo-do-curador");

        assertThat(resultado.cadeiaChamada).isTrue();
    }

    @Test
    @DisplayName("Listar obstáculos (GET) nunca exige token, mesmo sob o mesmo prefixo de URL")
    void listarNaoExigeToken() throws Exception {
        var resultado = executar(new FiltroAutenticacaoCurador("segredo-do-curador"), "GET", "/api/v1/obstaculos", null);

        assertThat(resultado.cadeiaChamada).isTrue();
    }

    @Test
    @DisplayName("Reportar um obstáculo (POST sem /validacao) nunca exige token")
    void reportarNaoExigeToken() throws Exception {
        var resultado = executar(new FiltroAutenticacaoCurador("segredo-do-curador"), "POST", "/api/v1/obstaculos", null);

        assertThat(resultado.cadeiaChamada).isTrue();
    }

    private Resultado executar(FiltroAutenticacaoCurador filtro, String metodo, String uri, String token) throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest(metodo, uri);
        if (token != null) {
            requisicao.addHeader(FiltroAutenticacaoCurador.CABECALHO_TOKEN, token);
        }
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        RegistraChamada cadeia = new RegistraChamada();

        filtro.doFilter(requisicao, resposta, cadeia);

        return new Resultado(resposta, cadeia.chamada);
    }

    private record Resultado(MockHttpServletResponse resposta, boolean cadeiaChamada) {
    }

    /** MockFilterChain não expõe se foi chamada; esta variante registra a chamada. */
    private static class RegistraChamada extends MockFilterChain {
        boolean chamada = false;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            chamada = true;
        }
    }
}
