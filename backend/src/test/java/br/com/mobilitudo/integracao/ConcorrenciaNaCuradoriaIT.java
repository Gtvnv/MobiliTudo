package br.com.mobilitudo.integracao;

import br.com.mobilitudo.curadores.infraestrutura.web.LoginRespostaDTO;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, com duas requisições de verdade disparadas ao mesmo tempo (não só sequenciais),
 * que a trava pessimista em {@code GerenciarObstaculos.aplicar} fecha a corrida que
 * existia antes: VALIDAR e REJEITAR no mesmo obstáculo, simultâneos, não podiam os dois
 * dar 200 — exatamente o cenário de "última escrita vence em silêncio" que motivou a
 * trava.
 *
 * <p>Usa um {@link CountDownLatch} para soltar as duas threads o mais perto possível uma
 * da outra, em vez de confiar em timing de rede — a sincronização explícita é o que torna
 * este teste determinístico em vez de só "geralmente reproduz a corrida".</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcorrenciaNaCuradoriaIT {

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

    @Test
    @DisplayName("VALIDAR e REJEITAR simultâneos no mesmo obstáculo: exatamente um 200, o outro 409 — nunca os dois 200")
    void duasTransicoesSimultaneasNuncaDevolvemAsDuasComSucesso() throws Exception {
        String obstaculoId = reportarObstaculoDeTeste();
        String token = tokenDeSessao();

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<Map>> tentativaValidar = executor.submit(() -> {
                aguardarLargada(largada);
                return avaliar(obstaculoId, "VALIDAR", token);
            });
            Future<ResponseEntity<Map>> tentativaRejeitar = executor.submit(() -> {
                aguardarLargada(largada);
                return avaliar(obstaculoId, "REJEITAR", token);
            });

            largada.countDown();

            ResponseEntity<Map> respostaValidar = tentativaValidar.get(20, TimeUnit.SECONDS);
            ResponseEntity<Map> respostaRejeitar = tentativaRejeitar.get(20, TimeUnit.SECONDS);

            List<HttpStatus> statusObtidos = List.of(
                    HttpStatus.valueOf(respostaValidar.getStatusCode().value()),
                    HttpStatus.valueOf(respostaRejeitar.getStatusCode().value()));

            // A ordem de quem "ganha" a corrida não é determinística — o que importa é
            // que exatamente uma das duas teve sucesso (200) e a outra encontrou um
            // obstáculo que não estava mais PENDENTE (409). Qualquer resultado diferente
            // disso (os dois 200, por exemplo) seria a corrida de volta.
            assertThat(statusObtidos).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
        } finally {
            executor.shutdownNow();
        }

        // O estado final tem que ser um dos dois, nunca "nenhum dos dois aconteceu" nem
        // um terceiro estado — confirma que a transição vencedora realmente persistiu.
        ResponseEntity<ObstaculoDTO[]> listaValidados = http.getForEntity(
                "/api/v1/obstaculos?status=VALIDADO", ObstaculoDTO[].class);
        ResponseEntity<ObstaculoDTO[]> listaRejeitados = http.getForEntity(
                "/api/v1/obstaculos?status=REJEITADO", ObstaculoDTO[].class);

        boolean ficouValidado = java.util.Arrays.stream(listaValidados.getBody())
                .anyMatch(o -> o.id().toString().equals(obstaculoId));
        boolean ficouRejeitado = java.util.Arrays.stream(listaRejeitados.getBody())
                .anyMatch(o -> o.id().toString().equals(obstaculoId));

        assertThat(ficouValidado ^ ficouRejeitado)
                .as("o obstáculo precisa ter ficado em exatamente um dos dois estados, nunca os dois nem nenhum")
                .isTrue();
    }

    private void aguardarLargada(CountDownLatch largada) {
        try {
            largada.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
        }
    }

    private String reportarObstaculoDeTeste() {
        Map<String, Object> corpo = Map.of(
                "tipo", "BURACO",
                "severidade", "LEVE",
                "coordenada", Map.of("lat", -23.9925, "lng", -46.4085),
                "descricao", "Teste de concorrência na curadoria",
                "reportadoPor", "teste-automatizado");
        ObstaculoDTO criado = http.postForEntity("/api/v1/obstaculos", corpo, ObstaculoDTO.class).getBody();
        return criado.id().toString();
    }

    private String tokenDeSessao() {
        ResponseEntity<LoginRespostaDTO> resposta = http.postForEntity("/api/v1/curadores/login",
                Map.of("email", EMAIL_DE_TESTE, "senha", SENHA_DE_TESTE), LoginRespostaDTO.class);
        return resposta.getBody().token();
    }

    private ResponseEntity<Map> avaliar(String id, String acao, String token) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.add("Authorization", "Bearer " + token);
        return http.exchange("/api/v1/obstaculos/" + id + "/validacao", HttpMethod.POST,
                new HttpEntity<>(Map.of("acao", acao), cabecalhos), Map.class);
    }
}
