package br.com.mobilitudo.curadores.infraestrutura.persistencia;

import br.com.mobilitudo.curadores.dominio.modelo.SessaoDeCurador;
import br.com.mobilitudo.curadores.dominio.porta.RepositorioSessoesCurador;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class SessoesCuradorRepositorioPostGis implements RepositorioSessoesCurador {

    private final SessaoCuradorJpaRepository jpa;

    SessoesCuradorRepositorioPostGis(SessaoCuradorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(SessaoDeCurador sessao, String hashToken) {
        jpa.save(SessaoCuradorEntidade.de(sessao, hashToken));
    }

    @Override
    public Optional<SessaoDeCurador> porHashToken(String hashToken) {
        return jpa.findByHashToken(hashToken).map(SessaoCuradorEntidade::paraDominio);
    }

    @Override
    public void remover(UUID sessaoId) {
        jpa.deleteById(sessaoId);
    }
}
