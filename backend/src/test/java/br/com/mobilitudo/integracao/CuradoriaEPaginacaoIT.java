package br.com.mobilitudo.integracao;

import br.com.mobilitudo.curadores.infraestrutura.web.LoginRespostaDTO;
import br.com.mobilitudo.obstaculos.infraestrutura.web.AvaliacaoDTO;
import br.com.mobilitudo.obstaculos.infraestrutura.web.ObstaculoDTO;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre, contra um PostgreSQL real, o que os testes unitários de {@code Curador} e
 * {@code Paginacao} não alcançam sozinhos: que o login por conta de curador está de fato
 * ligado ao endpoint de validação, e que a paginação funciona de ponta a ponta pela API.
 *
 * <p>O limite de relatos por hora é configurado bem alto aqui — só o suficiente para as
 * chamadas que este arquivo faz — porque o próprio limite tem um teste dedicado e isolado
 * em {@link LimiteDeObstaculosIT}: colocar os dois num mesmo contexto Spring faria os
 * relatos de um teste consumirem a cota verificada por outro, já que o limitador é um
 * singleton compartilhado por toda a classe.</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CuradoriaEPaginacaoIT {

    private static final String EMAIL_DE_TESTE = "curadora-de-teste@mobilitudo.org";
    private static final String SENHA_DE_TESTE = "senha-de-teste-123";

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
        registro.add("mobilitudo.seguranca.curador-inicial-email", () -> EMAIL_DE_TESTE);
        registro.add("mobilitudo.seguranca.curador-inicial-senha", () -> SENHA_DE_TESTE);
        registro.add("mobilitudo.rate-limit.obstaculos-por-hora", () -> 1000);
    }

    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void aguardarApi() {
        ResponseEntity<String> saude = http.getForEntity("/actuator/health", String.class);
        assertThat(saude.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ------------------------------------------------------------------
    // Autenticação da curadoria
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Validar sem o cabeçalho de token responde 401")
    void validarSemTokenERecusado() {
        ObstaculoDTO obstaculo = reportarObstaculoDeTeste();

        ResponseEntity<Map> resposta = avaliar(obstaculo.id(), AvaliacaoDTO.Acao.VALIDAR, null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Validar com token errado responde 401")
    void validarComTokenErradoERecusado() {
        ObstaculoDTO obstaculo = reportarObstaculoDeTeste();

        ResponseEntity<Map> resposta = avaliar(obstaculo.id(), AvaliacaoDTO.Acao.VALIDAR, "token-chutado");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Validar com uma sessão de curador válida funciona, e o relato passa a afetar rotas")
    void validarComTokenCorretoFunciona() {
        ObstaculoDTO obstaculo = reportarObstaculoDeTeste();

        ResponseEntity<ObstaculoDTO> resposta = avaliarComoObstaculoDTO(
                obstaculo.id(), AvaliacaoDTO.Acao.VALIDAR, tokenDeSessao());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().status()).isEqualTo("VALIDADO");
        assertThat(resposta.getBody().afetaRotas()).isTrue();
        assertThat(resposta.getBody().avaliadoPorEmail()).isEqualTo(EMAIL_DE_TESTE);
    }

    @Test
    @DisplayName("Reportar um obstáculo nunca exige o token de curadoria")
    void reportarNaoExigeToken() {
        ObstaculoDTO obstaculo = reportarObstaculoDeTeste();

        assertThat(obstaculo.status()).isEqualTo("PENDENTE");
    }

    // ------------------------------------------------------------------
    // Paginação
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Página cheia de locais traz o cabeçalho X-Tem-Proxima-Pagina = true")
    void listarLocaisComPaginaPequenaIndicaProximaPagina() {
        // O acervo semeado tem 4 locais (V2__dados_iniciais_praia_grande.sql).
        ResponseEntity<List> resposta = http.getForEntity("/api/v1/locais?pagina=0&tamanho=2", List.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(2);
        assertThat(resposta.getHeaders().getFirst("X-Tem-Proxima-Pagina")).isEqualTo("true");
    }

    @Test
    @DisplayName("Última página de locais traz o cabeçalho X-Tem-Proxima-Pagina = false")
    void ultimaPaginaDeLocaisNaoIndicaProximaPagina() {
        ResponseEntity<List> resposta = http.getForEntity("/api/v1/locais?pagina=1&tamanho=3", List.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(1);
        assertThat(resposta.getHeaders().getFirst("X-Tem-Proxima-Pagina")).isEqualTo("false");
    }

    @Test
    @DisplayName("Tamanho de página acima do máximo responde 400")
    void tamanhoDePaginaAcimaDoMaximoERecusado() {
        ResponseEntity<Map> resposta = http.getForEntity("/api/v1/locais?tamanho=500", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------

    private ObstaculoDTO reportarObstaculoDeTeste() {
        return http.postForEntity("/api/v1/obstaculos", corpoDeNovoObstaculo(), ObstaculoDTO.class).getBody();
    }

    private Map<String, Object> corpoDeNovoObstaculo() {
        return Map.of(
                "tipo", "BURACO",
                "severidade", "LEVE",
                "coordenada", Map.of("lat", -23.9925, "lng", -46.4085),
                "descricao", "Relato de teste de integração",
                "reportadoPor", "teste-automatizado");
    }

    private String tokenDeSessao() {
        ResponseEntity<LoginRespostaDTO> resposta = http.postForEntity("/api/v1/curadores/login",
                Map.of("email", EMAIL_DE_TESTE, "senha", SENHA_DE_TESTE), LoginRespostaDTO.class);
        return resposta.getBody().token();
    }

    private ResponseEntity<Map> avaliar(UUID id, AvaliacaoDTO.Acao acao, String token) {
        HttpHeaders cabecalhos = new HttpHeaders();
        if (token != null) {
            cabecalhos.add("Authorization", "Bearer " + token);
        }
        return http.exchange("/api/v1/obstaculos/" + id + "/validacao", HttpMethod.POST,
                new HttpEntity<>(Map.of("acao", acao.name()), cabecalhos), Map.class);
    }

    private ResponseEntity<ObstaculoDTO> avaliarComoObstaculoDTO(UUID id, AvaliacaoDTO.Acao acao, String token) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.add("Authorization", "Bearer " + token);
        return http.exchange("/api/v1/obstaculos/" + id + "/validacao", HttpMethod.POST,
                new HttpEntity<>(Map.of("acao", acao.name()), cabecalhos), ObstaculoDTO.class);
    }
}
