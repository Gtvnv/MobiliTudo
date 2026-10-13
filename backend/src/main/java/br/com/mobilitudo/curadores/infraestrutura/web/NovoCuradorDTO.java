package br.com.mobilitudo.curadores.infraestrutura.web;

import jakarta.validation.constraints.NotBlank;

/** Pedido de cadastro de uma nova conta de curador, feito por um curador já autenticado. */
public record NovoCuradorDTO(@NotBlank String email, @NotBlank String nomeDeExibicao, @NotBlank String senha) {
}
