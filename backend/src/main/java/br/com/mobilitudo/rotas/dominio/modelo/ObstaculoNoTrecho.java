package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;

import java.util.Objects;

/**
 * Projecao enxuta de um obstaculo validado, do ponto de vista do calculo de rotas.
 *
 * <p>O agregado completo (quem reportou, quando, status de validacao, foto) pertence ao
 * contexto de obstaculos. O roteamento so precisa saber o que atrapalha e o quanto,
 * e e por isso que a travessia desta fronteira perde os demais campos.</p>
 */
public record ObstaculoNoTrecho(TipoObstaculo tipo, Severidade severidade) {

    public ObstaculoNoTrecho {
        Objects.requireNonNull(tipo, "tipo do obstaculo e obrigatorio");
        Objects.requireNonNull(severidade, "severidade do obstaculo e obrigatoria");
    }
}
