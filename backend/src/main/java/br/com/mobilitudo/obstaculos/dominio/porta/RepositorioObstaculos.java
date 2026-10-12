package br.com.mobilitudo.obstaculos.dominio.porta;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Porta de saida para a persistencia de obstaculos reportados. */
public interface RepositorioObstaculos {

    Obstaculo salvar(Obstaculo obstaculo);

    Optional<Obstaculo> porId(UUID id);

    List<Obstaculo> porStatus(StatusValidacao status);

    /** Obstaculos dentro do raio informado, qualquer que seja o status. */
    List<Obstaculo> naArea(Coordenada centro, double raioMetros);
}
