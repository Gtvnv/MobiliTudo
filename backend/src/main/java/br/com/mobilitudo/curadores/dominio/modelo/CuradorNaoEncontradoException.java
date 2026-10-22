package br.com.mobilitudo.curadores.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

import java.util.UUID;

/** Curador inexistente. */
public class CuradorNaoEncontradoException extends ExcecaoDeDominio {

    public CuradorNaoEncontradoException(UUID id) {
        super("Curador " + id + " não encontrado");
    }
}
