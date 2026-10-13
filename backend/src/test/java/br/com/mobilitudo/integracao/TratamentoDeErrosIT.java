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
 * Cobre que uma entrada malformada vira erro do cliente (400, mensagem limpa), nunca o
 * texto bruto de uma exceção interna do Spring/JDK vazando pra fora — ver o porquê
 * completo em {@code ManipuladorGlobalDeErros.parametroComFormatoInvalido} e
 * {@code .erroInesperado}.
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TratamentoDeErrosIT {

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
    }

    @Autowired
    private TestRestTemplate http;

    @BeforeEach
    void aguardarApi() {
        ResponseEntity<String> saude = http.getForEntity("/actuator/health", String.class);
        assertThat(saude.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Id com formato inválido responde 400 com mensagem limpa, não o texto bruto da exceção do JDK")
    void idInvalidoNaoVazaMensagemInterna() {
        ResponseEntity<Map> resposta = http.postForEntity(
                "/api/v1/obstaculos/isto-nao-e-um-uuid/validacao",
                Map.of("acao", "VALIDAR"), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String detalhe = (String) resposta.getBody().get("detail");
        assertThat(detalhe).isEqualTo("Parâmetro 'id' tem formato inválido.");
        // A mensagem bruta do java.util.UUID ("Invalid UUID string: ...") e qualquer
        // referência ao tipo interno não devem aparecer na resposta.
        assertThat(detalhe).doesNotContain("UUID string").doesNotContain("java.util");
    }

    @Test
    @DisplayName("Raio com formato inválido na busca por proximidade também responde 400 limpo")
    void raioInvalidoNaoVazaMensagemInterna() {
        ResponseEntity<Map> resposta = http.getForEntity(
                "/api/v1/locais?lat=-23.99&lng=-46.41&raio=nao-e-um-numero", Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String detalhe = (String) resposta.getBody().get("detail");
        assertThat(detalhe).contains("raio").doesNotContain("NumberFormatException");
    }
}
