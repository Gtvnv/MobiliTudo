package br.com.mobilitudo.curadores.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

/** Senha curta demais para uma conta de curador. */
public class SenhaFracaException extends ExcecaoDeDominio {

    public SenhaFracaException() {
        super("A senha precisa ter pelo menos 8 caracteres.");
    }
}
