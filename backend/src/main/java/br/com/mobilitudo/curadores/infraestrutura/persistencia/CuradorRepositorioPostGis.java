package br.com.mobilitudo.curadores.infraestrutura.persistencia;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import br.com.mobilitudo.curadores.dominio.porta.RepositorioCuradores;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Sem geometria nenhuma aqui — o nome "PostGis" só segue a convenção dos outros adaptadores do projeto. */
@Repository
class CuradorRepositorioPostGis implements RepositorioCuradores {

    private final CuradorJpaRepository jpa;

    CuradorRepositorioPostGis(CuradorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Curador salvar(Curador curador) {
        return jpa.save(CuradorEntidade.de(curador)).paraDominio();
    }

    @Override
    public Optional<Curador> porId(UUID id) {
        return jpa.findById(id).map(CuradorEntidade::paraDominio);
    }

    @Override
    public Optional<Curador> porEmail(String email) {
        return jpa.findByEmail(email).map(CuradorEntidade::paraDominio);
    }

    @Override
    public boolean existeAlgum() {
        return jpa.count() > 0;
    }

    @Override
    public List<Curador> todos() {
        return jpa.findAllByOrderByCriadoEmAsc().stream().map(CuradorEntidade::paraDominio).toList();
    }
}
