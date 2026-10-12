package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Registra os filtros de segurança do contexto de obstáculos.
 *
 * <p>Os dois ficam sob {@code /api/v1/obstaculos/*}, mais amplo do que as rotas que cada
 * um de fato protege, porque padrão de URL de servlet não aceita curinga no meio do
 * caminho; cada filtro restringe o próprio efeito ao método e sufixo certos — ver
 * {@link FiltroAutenticacaoCurador} e {@link FiltroLimiteDeObstaculos}.</p>
 */
@Configuration
class ConfiguracaoSeguranca {

    @Bean
    FilterRegistrationBean<FiltroAutenticacaoCurador> filtroDeCuradoria(
            @Value("${mobilitudo.seguranca.token-curador:}") String tokenCurador) {
        FilterRegistrationBean<FiltroAutenticacaoCurador> registro =
                new FilterRegistrationBean<>(new FiltroAutenticacaoCurador(tokenCurador));
        registro.addUrlPatterns("/api/v1/obstaculos/*");
        registro.setName("filtroAutenticacaoCurador");
        // Os dois filtros atuam sobre rotas disjuntas (.../validacao vs. o POST exato de
        // relato), então a ordem relativa entre eles não muda o comportamento; fica
        // explícita para não depender da ordem de declaração dos beans.
        registro.setOrder(10);
        return registro;
    }

    @Bean
    FilterRegistrationBean<FiltroLimiteDeObstaculos> filtroDeLimiteDeObstaculos(
            @Value("${mobilitudo.rate-limit.obstaculos-por-hora:20}") int limitePorHora) {
        LimitadorDeTaxa limitador = new LimitadorDeTaxa(limitePorHora, Duration.ofHours(1));
        FilterRegistrationBean<FiltroLimiteDeObstaculos> registro =
                new FilterRegistrationBean<>(new FiltroLimiteDeObstaculos(limitador));
        registro.addUrlPatterns("/api/v1/obstaculos/*");
        registro.setName("filtroLimiteDeObstaculos");
        registro.setOrder(20);
        return registro;
    }
}
