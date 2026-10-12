package br.com.mobilitudo.obstaculos.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

import java.util.UUID;

/** Obstaculo inexistente ou ja removido do acervo. */
public class ObstaculoNaoEncontradoException extends ExcecaoDeDominio {

    public ObstaculoNaoEncontradoException(UUID id) {
        super("Obstaculo " + id + " nao encontrado");
    }
}
