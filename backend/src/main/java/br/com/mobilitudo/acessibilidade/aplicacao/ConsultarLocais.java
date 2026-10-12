package br.com.mobilitudo.acessibilidade.aplicacao;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.porta.RepositorioLocais;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PaginaDeResultado;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
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

    public PaginaDeResultado<Local> todos(Paginacao paginacao) {
        return PaginaDeResultado.buscar(paginacao, repositorio::todos);
    }

    public Optional<Local> porId(UUID id) {
        return repositorio.porId(id);
    }

    public PaginaDeResultado<Local> porCategoria(CategoriaLocal categoria, Paginacao paginacao) {
        return categoria == null
                ? todos(paginacao)
                : PaginaDeResultado.buscar(paginacao, p -> repositorio.porCategoria(categoria, p));
    }

    public PaginaDeResultado<Local> proximosDe(Coordenada referencia, double raioMetros, Paginacao paginacao) {
        return PaginaDeResultado.buscar(paginacao, p -> repositorio.proximosDe(referencia, raioMetros, p));
    }

    /**
     * Locais proximos que nao sao inacessiveis para o perfil informado.
     *
     * <p>O filtro deliberadamente mantem os locais classificados como "com ressalvas":
     * esconder tudo que nao seja perfeito deixaria o mapa vazio e tiraria do usuario a
     * decisao sobre o que ele consegue ou nao enfrentar.</p>
     *
     * <p>A paginacao e aplicada antes deste filtro, na consulta ao banco. Isso significa
     * que uma pagina pode voltar com menos itens do que o tamanho pedido quando alguns dos
     * locais daquela pagina sao inacessiveis ao perfil — nao ha reposicao buscando a
     * pagina seguinte dentro da mesma chamada. Para o volume atual do acervo e aceitavel;
     * se isso incomodar com um acervo grande, a filtragem por perfil precisaria migrar
     * para dentro da consulta SQL.</p>
     */
    public PaginaDeResultado<Local> proximosCompativeisCom(Coordenada referencia, double raioMetros,
                                                            PerfilMobilidade perfil, Paginacao paginacao) {
        PaginaDeResultado<Local> pagina = proximosDe(referencia, raioMetros, paginacao);
        List<Local> filtrados = pagina.itens().stream()
                .filter(local -> local.nivelPara(perfil) != NivelAcessibilidade.INACESSIVEL)
                .toList();
        return new PaginaDeResultado<>(filtrados, pagina.temProximaPagina());
    }
}
