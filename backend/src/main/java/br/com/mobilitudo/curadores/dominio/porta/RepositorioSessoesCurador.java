package br.com.mobilitudo.curadores.dominio.porta;

import br.com.mobilitudo.curadores.dominio.modelo.SessaoDeCurador;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioSessoesCurador {

    void salvar(SessaoDeCurador sessao, String hashToken);

    /** Localiza a sessão pelo hash do token apresentado — nunca pelo texto puro. */
    Optional<SessaoDeCurador> porHashToken(String hashToken);

    void remover(UUID sessaoId);
}
