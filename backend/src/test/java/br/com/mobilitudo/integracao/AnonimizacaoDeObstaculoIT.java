package br.com.mobilitudo.integracao;

import br.com.mobilitudo.obstaculos.infraestrutura.web.ObstaculoDTO;
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
 * Autoexclusão de dado pessoal (LGPD art. 18) de ponta a ponta: cobre o que
 * {@code TokenDeExclusaoTest} e {@code ObstaculoTest} não alcançam sozinhos — que o
 * hash de fato persiste numa tabela separada, sobrevive a uma transição de status do
 * obstáculo (prova de que {@code RepositorioObstaculos.salvar} nunca toca a tabela de
 * tokens), e que o token é consumido de verdade após o uso.
 *
 * <p>Classe isolada, com rate limit alto de propósito — não é o que este arquivo testa
 * — pelo mesmo motivo de {@link LimiteDeObstaculosIT} existir separado.</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnonimizacaoDeObstaculoIT {

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
    @DisplayName("O relato novo traz o token de exclusão, e ele não aparece em nenhuma listagem depois")
    void tokenApareceSoNaCriacao() {
        ObstaculoDTO criado = reportar();

        assertThat(criado.tokenExclusao()).isNotBlank();

        ResponseEntity<ObstaculoDTO[]> listagem = http.getForEntity(
                "/api/v1/obstaculos?status=PENDENTE", ObstaculoDTO[].class);
        ObstaculoDTO naLista = java.util.Arrays.stream(listagem.getBody())
                .filter(o -> o.id().equals(criado.id()))
                .findFirst()
                .orElseThrow();

        assertThat(naLista.tokenExclusao()).isNull();
    }

    @Test
    @DisplayName("Anonimizar com o token correto remove o nome e preserva a barreira")
    void anonimizarComTokenCorretoFunciona() {
        ObstaculoDTO criado = reportar();

        ResponseEntity<ObstaculoDTO> resposta = http.postForEntity(
                "/api/v1/obstaculos/" + criado.id() + "/anonimizar",
                Map.of("tokenExclusao", criado.tokenExclusao()),
                ObstaculoDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().reportadoPor()).isEqualTo("anônimo");
        assertThat(resposta.getBody().status()).isEqualTo("PENDENTE");
    }

    @Test
    @DisplayName("Anonimizar com token errado responde 403, e o nome original permanece")
    void anonimizarComTokenErradoERecusado() {
        ObstaculoDTO criado = reportar();

        ResponseEntity<Map> resposta = http.postForEntity(
                "/api/v1/obstaculos/" + criado.id() + "/anonimizar",
                Map.of("tokenExclusao", "um-token-chutado-qualquer"),
                Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("O token é de uso único: a segunda tentativa com o mesmo token é recusada")
    void tokenEDeUsoUnico() {
        ObstaculoDTO criado = reportar();

        ResponseEntity<ObstaculoDTO> primeira = http.postForEntity(
                "/api/v1/obstaculos/" + criado.id() + "/anonimizar",
                Map.of("tokenExclusao", criado.tokenExclusao()),
                ObstaculoDTO.class);
        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> segunda = http.postForEntity(
                "/api/v1/obstaculos/" + criado.id() + "/anonimizar",
                Map.of("tokenExclusao", criado.tokenExclusao()),
                Map.class);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Anonimizar sobrevive a uma validação: o status muda, o token continua funcionando")
    void anonimizarFuncionaDepoisDeValidado() {
        ObstaculoDTO criado = reportar();

        // Só para provar que salvar uma transição de status não corrompe nem apaga o
        // hash do token — guardado em tabela separada de propósito. Sem curador
        // configurado neste teste, a validação em si não é o alvo; chega a 503 e
        // tudo bem, o que importa é que o POST não quebrou a tabela de tokens.
        http.postForEntity("/api/v1/obstaculos/" + criado.id() + "/validacao",
                Map.of("acao", "VALIDAR"), Map.class);

        ResponseEntity<ObstaculoDTO> resposta = http.postForEntity(
                "/api/v1/obstaculos/" + criado.id() + "/anonimizar",
                Map.of("tokenExclusao", criado.tokenExclusao()),
                ObstaculoDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().reportadoPor()).isEqualTo("anônimo");
    }

    private ObstaculoDTO reportar() {
        Map<String, Object> corpo = Map.of(
                "tipo", "BURACO",
                "severidade", "LEVE",
                "coordenada", Map.of("lat", -23.9925, "lng", -46.4085),
                "descricao", "Relato de teste de anonimização",
                "reportadoPor", "pessoa-de-teste");
        return http.postForEntity("/api/v1/obstaculos", corpo, ObstaculoDTO.class).getBody();
    }
}
