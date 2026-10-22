package br.com.mobilitudo.acessibilidade.aplicacao;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.modelo.RecursoAcessibilidade;
import br.com.mobilitudo.acessibilidade.dominio.porta.RepositorioLocais;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

/** Caso de uso: cadastrar um novo ponto acessivel no mapa colaborativo. */
@Service
public class RegistrarLocal {

    private final RepositorioLocais repositorio;

    public RegistrarLocal(RepositorioLocais repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Local executar(Comando comando) {
        Local local = new Local(
                null,
                comando.nome(),
                comando.categoria(),
                comando.coordenada(),
                comando.endereco(),
                comando.descricao(),
                comando.imagemUrl(),
                comando.recursos(),
                Instant.now());
        return repositorio.salvar(local);
    }

    public record Comando(String nome,
                          CategoriaLocal categoria,
                          Coordenada coordenada,
                          String endereco,
                          String descricao,
                          String imagemUrl,
                          Set<RecursoAcessibilidade> recursos) {
    }
}
