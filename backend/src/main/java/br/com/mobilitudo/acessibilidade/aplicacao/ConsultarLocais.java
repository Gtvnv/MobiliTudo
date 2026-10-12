package br.com.mobilitudo.acessibilidade.aplicacao;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.porta.RepositorioLocais;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de consulta ao acervo de locais mapeados. */
@Service
@Transactional(readOnly = true)
public class ConsultarLocais {

    private final RepositorioLocais repositorio;

    public ConsultarLocais(RepositorioLocais repositorio) {
        this.repositorio = repositorio;
    }

    public List<Local> todos() {
        return repositorio.todos();
    }

    public Optional<Local> porId(UUID id) {
        return repositorio.porId(id);
    }

    public List<Local> porCategoria(CategoriaLocal categoria) {
        return categoria == null ? repositorio.todos() : repositorio.porCategoria(categoria);
    }

    public List<Local> proximosDe(Coordenada referencia, double raioMetros) {
        return repositorio.proximosDe(referencia, raioMetros);
    }

    /**
     * Locais proximos que nao sao inacessiveis para o perfil informado.
     *
     * <p>O filtro deliberadamente mantem os locais classificados como "com ressalvas":
     * esconder tudo que nao seja perfeito deixaria o mapa vazio e tiraria do usuario a
     * decisao sobre o que ele consegue ou nao enfrentar.</p>
     */
    public List<Local> proximosCompativeisCom(Coordenada referencia, double raioMetros, PerfilMobilidade perfil) {
        return repositorio.proximosDe(referencia, raioMetros).stream()
                .filter(local -> local.nivelPara(perfil) != NivelAcessibilidade.INACESSIVEL)
                .toList();
    }
}
