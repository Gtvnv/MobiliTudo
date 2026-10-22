package br.com.mobilitudo.integracao;

import br.com.mobilitudo.rotas.infraestrutura.web.RotaDTO;
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
 * Teste de ponta a ponta contra um PostgreSQL com PostGIS de verdade.
 *
 * <p>E o unico lugar que verifica o que um teste unitario nao alcanca: as migracoes Flyway,
 * o mapeamento das colunas {@code geometry}, as consultas espaciais nativas e a juncao dos
 * obstaculos de outro contexto a malha viaria.</p>
 *
 * <p>Fica fora do build padrao (marcado com {@code integracao}) porque depende de Docker.
 * Para executar: {@code mvn verify -Dgroups=integracao}.</p>
 */
@Tag("integracao")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RotaAcessivelIT {

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

    private static final Map<String, Double> PALACIO_DAS_ARTES = Map.of("lat", -23.9920, "lng", -46.4110);
    private static final Map<String, Double> LITORAL_PLAZA = Map.of("lat", -23.9930, "lng", -46.4070);

    @Test
    @DisplayName("As migracoes criam o esquema e carregam os quatro locais de Praia Grande")
    void carregaOAcervoInicial() {
        ResponseEntity<List<Map<String, Object>>> resposta = http.exchange(
                "/api/v1/locais", org.springframework.http.HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<>() {
                });

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(4)
                .anySatisfy(local -> assertThat(local.get("nome")).isEqualTo("Litoral Plaza Shopping"));
    }

    @Test
    @DisplayName("A busca por proximidade usa o indice espacial e respeita o raio em metros")
    void buscaPorProximidadeNoPostGis() {
        ResponseEntity<List<Map<String, Object>>> perto = http.exchange(
                "/api/v1/locais?lat=-23.9920&lng=-46.4110&raio=1000",
                org.springframework.http.HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<>() {
                });

        // Palacio das Artes e Litoral Plaza ficam dentro de 1 km; a praia da Guilhermina nao.
        assertThat(perto.getBody())
                .isNotNull()
                .allSatisfy(local -> assertThat((String) local.get("nome")).doesNotContain("Guilhermina"));
    }

    @Test
    @DisplayName("Sem restricao de mobilidade a rota segue a avenida, que e o caminho curto")
    void rotaPadraoSegueAAvenida() {
        RotaDTO rota = calcularRota("PADRAO");

        assertThat(rota.passos()).extracting(RotaDTO.PassoDTO::segmentoId)
                .containsExactly("av-1-2", "av-2-3", "av-3-4", "av-4-5");
        assertThat(rota.distanciaMetros()).isBetween(400.0, 450.0);
    }

    @Test
    @DisplayName("Em cadeira de rodas a obra e o degrau sao contornados pela rua paralela")
    void rotaEmCadeiraDeRodasContornaAsBarreiras() {
        RotaDTO curta = calcularRota("PADRAO");
        RotaDTO acessivel = calcularRota("CADEIRA_DE_RODAS");

        assertThat(acessivel.passos()).extracting(RotaDTO.PassoDTO::segmentoId)
                .containsExactly("av-1-2", "av-2-3", "cruz-3", "par-3-4", "cruz-4", "av-4-5");

        // O trecho com a obra validada e o atalho com degrau ficam de fora.
        assertThat(acessivel.passos()).extracting(RotaDTO.PassoDTO::segmentoId)
                .doesNotContain("av-3-4", "atalho-praca");

        assertThat(acessivel.distanciaMetros()).isGreaterThan(curta.distanciaMetros());
        assertThat(acessivel.nivelAcessibilidade()).isEqualTo("PLENO");
        assertThat(acessivel.geometria()).isNotEmpty();
    }

    @Test
    @DisplayName("Obstaculo ainda pendente de conferencia nao desvia rota de ninguem")
    void relatoPendenteNaoAfetaORoteamento() {
        RotaDTO acessivel = calcularRota("CADEIRA_DE_RODAS");

        // par-3-4 tem um relato PENDENTE de degrau, que barraria uma cadeira de rodas se
        // estivesse confirmado. Como nao esta, o trecho segue na rota e sem ressalva alguma.
        assertThat(acessivel.passos())
                .filteredOn(passo -> passo.segmentoId().equals("par-3-4"))
                .singleElement()
                .satisfies(passo -> assertThat(passo.ressalvas()).isEmpty());
    }

    @Test
    @DisplayName("Ponto fora do recorte mapeado responde 404 dizendo exatamente o porque")
    void pontoForaDaAreaMapeada() {
        ResponseEntity<Map<String, Object>> resposta = http.exchange(
                "/api/v1/rotas/acessivel", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(Map.of(
                        "origem", Map.of("lat", -24.015, "lng", -46.398),
                        "destino", LITORAL_PLAZA,
                        "perfil", "CADEIRA_DE_RODAS")),
                new org.springframework.core.ParameterizedTypeReference<>() {
                });

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("motivo", "ORIGEM_FORA_DA_AREA_MAPEADA");
    }

    private RotaDTO calcularRota(String perfil) {
        ResponseEntity<RotaDTO> resposta = http.postForEntity("/api/v1/rotas/acessivel",
                Map.of("origem", PALACIO_DAS_ARTES, "destino", LITORAL_PLAZA, "perfil", perfil),
                RotaDTO.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }
}
