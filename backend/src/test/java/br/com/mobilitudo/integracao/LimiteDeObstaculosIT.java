package br.com.mobilitudo.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste dedicado e isolado do limite de relatos de obstáculo por hora.
 *
 * <p>Fica numa classe própria, com seu próprio contexto Spring, de propósito: o limitador
 * é um bean singleton, e se este teste dividisse contexto com
 * {@link CuradoriaEPaginacaoIT} os relatos feitos por aquela classe consumiriam a mesma
 * cota verificada aqui — foi exatamente esse acoplamento que quebrou a primeira versão
 * deste teste, com os dois num único arquivo.</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LimiteDeObstaculosIT {

    private static final int LIMITE_DE_TESTE = 2;

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
        registro.add("mobilitudo.rate-limit.obstaculos-por-hora", () -> LIMITE_DE_TESTE);
    }

    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void aguardarApi() {
        ResponseEntity<String> saude = http.getForEntity("/actuator/health", String.class);
        assertThat(saude.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Depois do limite configurado, a mesma origem recebe 429 em novos relatos")
    void excederOLimiteResponde429() {
        for (int i = 0; i < LIMITE_DE_TESTE; i++) {
            ResponseEntity<Map> permitido = http.postForEntity("/api/v1/obstaculos", corpoDeNovoObstaculo(), Map.class);
            assertThat(permitido.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        ResponseEntity<Map> excedente = http.postForEntity("/api/v1/obstaculos", corpoDeNovoObstaculo(), Map.class);

        assertThat(excedente.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Listar obstáculos não é afetado pelo limite de relatos")
    void listarNaoEAfetadoPeloLimite() {
        // GET devolve um array JSON, não um objeto: List.class, não Map.class.
        for (int i = 0; i < LIMITE_DE_TESTE + 3; i++) {
            ResponseEntity<List> resposta = http.getForEntity("/api/v1/obstaculos", List.class);
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    private Map<String, Object> corpoDeNovoObstaculo() {
        return Map.of(
                "tipo", "BURACO",
                "severidade", "LEVE",
                "coordenada", Map.of("lat", -23.9925, "lng", -46.4085),
                "descricao", "Relato de teste do limite de taxa",
                "reportadoPor", "teste-automatizado");
    }
}
