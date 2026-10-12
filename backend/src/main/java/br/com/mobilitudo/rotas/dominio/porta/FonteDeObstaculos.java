package br.com.mobilitudo.rotas.dominio.porta;

import br.com.mobilitudo.compartilhado.dominio.AreaRetangular;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;

import java.util.List;
import java.util.Map;

/**
 * Porta pela qual o roteamento enxerga os obstaculos reportados pela comunidade.
 *
 * <p>Existe para que o contexto de rotas nao leia a tabela de outro contexto. Quem
 * implementa decide o que e um obstaculo "para valer" — hoje, apenas os validados — e
 * entrega so o que o calculo precisa: tipo e severidade por trecho.</p>
 */
public interface FonteDeObstaculos {

    /** Obstaculos vigentes na area, indexados pelo id do trecho que eles afetam. */
    Map<String, List<ObstaculoNoTrecho>> porSegmentoNaArea(AreaRetangular area);
}
