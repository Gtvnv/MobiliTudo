package br.com.mobilitudo.obstaculos.infraestrutura.web;

import jakarta.validation.constraints.NotNull;

/** Decisao da curadoria sobre um relato. */
public record AvaliacaoDTO(@NotNull Acao acao) {

    public enum Acao {
        VALIDAR,
        REJEITAR,
        RESOLVER
    }
}
