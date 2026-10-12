package br.com.mobilitudo.acessibilidade.infraestrutura.persistencia;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.porta.RepositorioLocais;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de saida: implementa a porta do dominio sobre PostgreSQL + PostGIS. */
@Repository
class LocalRepositorioPostGis implements RepositorioLocais {

    private final LocalJpaRepository jpa;

    LocalRepositorioPostGis(LocalJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Local salvar(Local local) {
        return jpa.save(LocalEntidade.de(local)).paraDominio();
    }

    @Override
    public Optional<Local> porId(UUID id) {
        return jpa.findById(id).map(LocalEntidade::paraDominio);
    }

    @Override
    public List<Local> todos() {
        return jpa.findAllByOrderByNomeAsc().stream().map(LocalEntidade::paraDominio).toList();
    }

    @Override
    public List<Local> porCategoria(CategoriaLocal categoria) {
        return jpa.findAllByCategoriaOrderByNomeAsc(categoria).stream()
                .map(LocalEntidade::paraDominio)
                .toList();
    }

    @Override
    public List<Local> proximosDe(Coordenada referencia, double raioMetros) {
        return jpa.buscarProximos(referencia.latitude(), referencia.longitude(), raioMetros).stream()
                .map(LocalEntidade::paraDominio)
                .toList();
    }
}
