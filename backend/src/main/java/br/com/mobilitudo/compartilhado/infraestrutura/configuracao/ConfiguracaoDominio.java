package br.com.mobilitudo.compartilhado.infraestrutura.configuracao;

import br.com.mobilitudo.rotas.dominio.servico.AvaliadorAcessibilidade;
import br.com.mobilitudo.rotas.dominio.servico.CalculadoraRotaAcessivel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os servicos de dominio como beans.
 *
 * <p>As classes do dominio nao carregam anotacao de framework alguma — e a infraestrutura
 * que as conhece, nunca o contrario. O preco e esta configuracao explicita; o ganho e que
 * o nucleo de regras roda em teste unitario puro.</p>
 */
@Configuration
class ConfiguracaoDominio {

    @Bean
    AvaliadorAcessibilidade avaliadorAcessibilidade() {
        return new AvaliadorAcessibilidade();
    }

    @Bean
    CalculadoraRotaAcessivel calculadoraRotaAcessivel(AvaliadorAcessibilidade avaliador) {
        return new CalculadoraRotaAcessivel(avaliador, CalculadoraRotaAcessivel.RAIO_DE_APROXIMACAO_METROS);
    }
}
