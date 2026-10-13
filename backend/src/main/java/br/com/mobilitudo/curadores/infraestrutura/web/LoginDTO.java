package br.com.mobilitudo.curadores.infraestrutura.web;

import jakarta.validation.constraints.NotBlank;

/** Pedido de login de curador: e-mail e senha em texto puro, só nesta requisição. */
public record LoginDTO(@NotBlank String email, @NotBlank String senha) {
}
