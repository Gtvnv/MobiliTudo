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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste dedicado e isolado do limite de tentativas de login por hora — mesmo motivo de
 * {@code LimiteDeObstaculosIT}: o limitador é um singleton compartilhado por toda a
 * classe, então um contexto Spring próprio evita que outro teste consuma a cota
 * verificada aqui.
 *
 * <p>Diferente de {@code MOBILITUDO_CURADOR_TOKEN} (um segredo de alta entropia), uma
 * senha de curador está sujeita a força bruta — é esse risco que este limite contém.</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LimiteDeLoginIT {

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
        registro.add("mobilitudo.rate-limit.login-tentativas-por-hora", () -> LIMITE_DE_TESTE);
    }

    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void aguardarApi() {
        ResponseEntity<String> saude = http.getForEntity("/actuator/health", String.class);
        assertThat(saude.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Depois do limite configurado, a mesma origem recebe 429 em novas tentativas de login")
    void excederOLimiteResponde429() {
        for (int i = 0; i < LIMITE_DE_TESTE; i++) {
            ResponseEntity<Map> tentativa = login("alguem@mobilitudo.org", "senha-chutada-" + i);
            assertThat(tentativa.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<Map> excedente = login("alguem@mobilitudo.org", "mais-uma-tentativa");

        assertThat(excedente.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private ResponseEntity<Map> login(String email, String senha) {
        return http.postForEntity("/api/v1/curadores/login", Map.of("email", email, "senha", senha), Map.class);
    }
}
