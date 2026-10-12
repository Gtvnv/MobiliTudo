package br.com.mobilitudo.obstaculos.dominio.porta;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saida para a persistencia de obstaculos reportados. */
public interface RepositorioObstaculos {

    Obstaculo salvar(Obstaculo obstaculo);

    Optional<Obstaculo> porId(UUID id);

    List<Obstaculo> porStatus(StatusValidacao status, Paginacao paginacao);

    /**
     * Todos os obstáculos com o status informado, sem paginação.
     *
     * <p>Existe só para o adaptador que alimenta o cálculo de rota
     * ({@code FonteDeObstaculosValidados}): o roteamento precisa enxergar toda barreira
     * validada na área, porque uma página incompleta esconderia um obstáculo real do A* e
     * poderia fazer um perfil sobre rodas ser roteado direto para cima dele. Não é para uso
     * em endpoint de listagem — esses usam a versão paginada.</p>
     */
    List<Obstaculo> porStatusCompleto(StatusValidacao status);

    /** Obstaculos dentro do raio informado, qualquer que seja o status. */
    List<Obstaculo> naArea(Coordenada centro, double raioMetros, Paginacao paginacao);
}
