package br.com.mobilitudo.obstaculos.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

/** Mudanca de status que o ciclo de vida do obstaculo nao permite. */
public class TransicaoInvalidaException extends ExcecaoDeDominio {

    public TransicaoInvalidaException(StatusValidacao atual, StatusValidacao pretendido) {
        super("Obstaculo em %s nao pode ir para %s".formatted(atual, pretendido));
    }
}
