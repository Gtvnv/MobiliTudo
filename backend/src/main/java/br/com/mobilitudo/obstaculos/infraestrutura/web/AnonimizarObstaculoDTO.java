package br.com.mobilitudo.obstaculos.infraestrutura.web;

import jakarta.validation.constraints.NotBlank;

/** Pedido de autoexclusão do nome de quem relatou, mediante posse do token recebido na criação. */
public record AnonimizarObstaculoDTO(@NotBlank String tokenExclusao) {
}
