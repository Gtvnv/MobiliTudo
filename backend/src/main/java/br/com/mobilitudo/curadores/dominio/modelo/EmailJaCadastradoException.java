package br.com.mobilitudo.curadores.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

/** Já existe uma conta de curador com este e-mail. */
public class EmailJaCadastradoException extends ExcecaoDeDominio {

    public EmailJaCadastradoException(String email) {
        super("Já existe uma conta de curador com o e-mail " + email);
    }
}
