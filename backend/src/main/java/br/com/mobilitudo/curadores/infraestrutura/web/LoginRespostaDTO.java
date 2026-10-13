package br.com.mobilitudo.curadores.infraestrutura.web;

import br.com.mobilitudo.curadores.aplicacao.AutenticarCurador;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "LoginDeCurador", description = "Token de sessão — guarde-o para enviar como Authorization: Bearer <token>")
public record LoginRespostaDTO(String token, Instant expiraEm, CuradorDTO curador) {

    public static LoginRespostaDTO de(AutenticarCurador.ResultadoDeLogin resultado) {
        return new LoginRespostaDTO(resultado.tokenEmTextoPuro(), resultado.expiraEm(),
                CuradorDTO.de(resultado.curador()));
    }
}
