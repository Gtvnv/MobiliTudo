package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import br.com.mobilitudo.obstaculos.dominio.porta.RepositorioTokensDeExclusao;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class TokensDeExclusaoRepositorioPostGis implements RepositorioTokensDeExclusao {

    private final ObstaculoTokenExclusaoJpaRepository jpa;

    TokensDeExclusaoRepositorioPostGis(ObstaculoTokenExclusaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(UUID obstaculoId, String hashToken) {
        jpa.save(new ObstaculoTokenExclusaoEntidade(obstaculoId, hashToken));
    }

    @Override
    public Optional<String> hashDe(UUID obstaculoId) {
        return jpa.findById(obstaculoId).map(ObstaculoTokenExclusaoEntidade::hashToken);
    }

    @Override
    public void remover(UUID obstaculoId) {
        jpa.deleteById(obstaculoId);
    }
}
