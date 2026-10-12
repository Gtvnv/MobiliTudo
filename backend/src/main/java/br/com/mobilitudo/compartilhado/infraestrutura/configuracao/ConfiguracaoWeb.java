package br.com.mobilitudo.compartilhado.infraestrutura.configuracao;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS e documentacao da API.
 *
 * <p>As origens liberadas vem de {@code mobilitudo.cors.origens} para que o mapa possa ser
 * servido de um host diferente do da API (por exemplo, GitHub Pages consumindo um backend
 * em outro dominio) sem precisar recompilar.</p>
 */
@Configuration
class ConfiguracaoWeb implements WebMvcConfigurer {

    private final List<String> origensPermitidas;

    ConfiguracaoWeb(@Value("${mobilitudo.cors.origens}") List<String> origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOriginPatterns(origensPermitidas.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Bean
    OpenAPI documentacaoDaApi() {
        return new OpenAPI().info(new Info()
                .title("MobiliTudo API")
                .version("v1")
                .description("""
                        API geoespacial de acessibilidade urbana de Praia Grande: acervo de locais \
                        acessíveis, obstáculos reportados pela comunidade e cálculo de rotas \
                        que contornam barreiras segundo o perfil de mobilidade de quem caminha.""")
                .license(new License().name("MIT")));
    }
}
