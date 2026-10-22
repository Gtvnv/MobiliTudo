package br.com.mobilitudo.integracao;

import br.com.mobilitudo.curadores.infraestrutura.web.CuradorDTO;
import br.com.mobilitudo.curadores.infraestrutura.web.LoginRespostaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre, contra um PostgreSQL real, o ciclo de vida de uma conta de curador: bootstrap da
 * primeira conta, login (certo, errado, conta desativada), sessão (/me, logout) e gestão de
 * outras contas (cadastro, e-mail duplicado, desativação).
 *
 * <p>O limite de tentativas de login é configurado bem alto aqui, pelo mesmo motivo de
 * {@link CuradoriaEPaginacaoIT}: o teste do limite em si é isolado em
 * {@link LimiteDeLoginIT}, com seu próprio contexto Spring, porque o limitador é um
 * singleton compartilhado por toda a classe.</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContasDeCuradorIT {

    private static final String EMAIL_INICIAL = "curadora-inicial@mobilitudo.org";
    private static final String SENHA_INICIAL = "senha-inicial-123";

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mobilitudo")
            .withUsername("mobilitudo")
            .withPassword("mobilitudo");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGIS::getUsername);
        registro.add("spring.datasource.password", POSTGIS::getPassword);
        registro.add("mobilitudo.seguranca.curador-inicial-email", () -> EMAIL_INICIAL);
        registro.add("mobilitudo.seguranca.curador-inicial-senha", () -> SENHA_INICIAL);
        registro.add("mobilitudo.rate-limit.login-tentativas-por-hora", () -> 1000);
    }

    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void aguardarApi() {
        ResponseEntity<String> saude = http.getForEntity("/actuator/health", String.class);
        assertThat(saude.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Login com e-mail não cadastrado responde 401")
    void loginComEmailInexistenteERecusado() {
        ResponseEntity<Map> resposta = login("ninguem@mobilitudo.org", "qualquer-senha");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Login com senha errada responde 401, com a mesma mensagem de e-mail inexistente")
    void loginComSenhaErradaERecusado() {
        ResponseEntity<Map> comEmailErrado = login("ninguem@mobilitudo.org", "qualquer-senha");
        ResponseEntity<Map> comSenhaErrada = login(EMAIL_INICIAL, "senha-chutada");

        assertThat(comSenhaErrada.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(comSenhaErrada.getBody().get("detail")).isEqualTo(comEmailErrado.getBody().get("detail"));
    }

    @Test
    @DisplayName("Login com e-mail e senha corretos devolve um token de sessão")
    void loginComCredenciaisCorretasFunciona() {
        ResponseEntity<LoginRespostaDTO> resposta = http.postForEntity("/api/v1/curadores/login",
                Map.of("email", EMAIL_INICIAL, "senha", SENHA_INICIAL), LoginRespostaDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().token()).isNotBlank();
        assertThat(resposta.getBody().curador().email()).isEqualTo(EMAIL_INICIAL);
    }

    @Test
    @DisplayName("GET /me com o token da sessão devolve os dados da própria conta")
    void meComTokenValidoDevolveDadosDaConta() {
        String token = tokenDoCuradorInicial();

        ResponseEntity<CuradorDTO> resposta = http.exchange("/api/v1/curadores/me", HttpMethod.GET,
                autenticado(token), CuradorDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().email()).isEqualTo(EMAIL_INICIAL);
    }

    @Test
    @DisplayName("GET /me sem token responde 401")
    void meSemTokenERecusado() {
        ResponseEntity<Map> resposta = http.getForEntity("/api/v1/curadores/me", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Depois do logout, o mesmo token deixa de autenticar")
    void logoutInvalidaOToken() {
        String token = tokenDoCuradorInicial();

        ResponseEntity<Void> logout = http.exchange("/api/v1/curadores/logout", HttpMethod.POST,
                autenticado(token), Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> meDepoisDoLogout = http.exchange("/api/v1/curadores/me", HttpMethod.GET,
                autenticado(token), Map.class);
        assertThat(meDepoisDoLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Cadastrar um novo curador sem estar autenticado responde 401")
    void cadastrarSemAutenticacaoERecusado() {
        ResponseEntity<Map> resposta = http.postForEntity("/api/v1/curadores",
                Map.of("email", "novo@mobilitudo.org", "nomeDeExibicao", "Novo", "senha", "senha-nova-123"),
                Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Um curador autenticado cadastra outro, que já consegue logar")
    void curadorAutenticadoCadastraOutro() {
        String token = tokenDoCuradorInicial();

        ResponseEntity<CuradorDTO> cadastro = http.exchange("/api/v1/curadores", HttpMethod.POST,
                autenticado(token, Map.of("email", "segunda-curadora@mobilitudo.org",
                        "nomeDeExibicao", "Segunda Curadora", "senha", "senha-nova-123")),
                CuradorDTO.class);

        assertThat(cadastro.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cadastro.getBody().email()).isEqualTo("segunda-curadora@mobilitudo.org");

        ResponseEntity<LoginRespostaDTO> loginDaNova = http.postForEntity("/api/v1/curadores/login",
                Map.of("email", "segunda-curadora@mobilitudo.org", "senha", "senha-nova-123"),
                LoginRespostaDTO.class);
        assertThat(loginDaNova.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Cadastrar com um e-mail já em uso responde 409")
    void cadastrarComEmailDuplicadoERecusado() {
        String token = tokenDoCuradorInicial();

        ResponseEntity<Map> resposta = http.exchange("/api/v1/curadores", HttpMethod.POST,
                autenticado(token, Map.of("email", EMAIL_INICIAL, "nomeDeExibicao", "Outra", "senha", "senha-123456")),
                Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Uma conta desativada não consegue mais logar")
    void contaDesativadaNaoConsegueMaisLogar() {
        String token = tokenDoCuradorInicial();

        ResponseEntity<CuradorDTO> cadastro = http.exchange("/api/v1/curadores", HttpMethod.POST,
                autenticado(token, Map.of("email", "terceira-curadora@mobilitudo.org",
                        "nomeDeExibicao", "Terceira Curadora", "senha", "senha-nova-123")),
                CuradorDTO.class);
        var idDaTerceira = cadastro.getBody().id();

        ResponseEntity<CuradorDTO> desativacao = http.exchange("/api/v1/curadores/" + idDaTerceira + "/desativar",
                HttpMethod.POST, autenticado(token), CuradorDTO.class);
        assertThat(desativacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(desativacao.getBody().ativo()).isFalse();

        ResponseEntity<Map> loginDepoisDeDesativada = login("terceira-curadora@mobilitudo.org", "senha-nova-123");
        assertThat(loginDepoisDeDesativada.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String tokenDoCuradorInicial() {
        ResponseEntity<LoginRespostaDTO> resposta = http.postForEntity("/api/v1/curadores/login",
                Map.of("email", EMAIL_INICIAL, "senha", SENHA_INICIAL), LoginRespostaDTO.class);
        return resposta.getBody().token();
    }

    private ResponseEntity<Map> login(String email, String senha) {
        return http.postForEntity("/api/v1/curadores/login", Map.of("email", email, "senha", senha), Map.class);
    }

    private HttpEntity<Void> autenticado(String token) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.add("Authorization", "Bearer " + token);
        return new HttpEntity<>(cabecalhos);
    }

    private HttpEntity<Map<String, Object>> autenticado(String token, Map<String, Object> corpo) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.add("Authorization", "Bearer " + token);
        return new HttpEntity<>(corpo, cabecalhos);
    }
}
