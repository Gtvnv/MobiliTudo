package br.com.mobilitudo.curadores.dominio.modelo;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma sessão de login ativa. Não carrega o token nem o hash dele — isso é detalhe de
 * como a sessão é localizada no banco, não algo que o domínio precisa saber para
 * decidir se ela ainda vale.
 */
public record SessaoDeCurador(UUID id, UUID curadorId, Instant criadaEm, Instant expiraEm) {

    public boolean expirada(Instant agora) {
        return !agora.isBefore(expiraEm);
    }
}
