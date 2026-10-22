package br.com.mobilitudo.curadores.infraestrutura.web;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "Curador", description = "Conta de acesso à curadoria de obstáculos — nunca inclui a senha ou seu hash")
public record CuradorDTO(UUID id, String email, String nomeDeExibicao, boolean ativo, Instant criadoEm) {

    public static CuradorDTO de(Curador curador) {
        return new CuradorDTO(curador.id(), curador.email(), curador.nomeDeExibicao(),
                curador.ativo(), curador.criadoEm());
    }
}
