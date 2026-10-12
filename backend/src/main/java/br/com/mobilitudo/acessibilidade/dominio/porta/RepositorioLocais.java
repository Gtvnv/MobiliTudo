package br.com.mobilitudo.acessibilidade.dominio.porta;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saida para a persistencia de locais.
 *
 * <p>Declarada em termos do dominio: quem implementa decide se a busca por proximidade vira
 * {@code ST_DWithin} no PostGIS ou um filtro em memoria num teste.</p>
 */
public interface RepositorioLocais {

    Local salvar(Local local);

    Optional<Local> porId(UUID id);

    List<Local> todos();

    List<Local> porCategoria(CategoriaLocal categoria);

    /** Locais dentro do raio informado, ordenados do mais proximo ao mais distante. */
    List<Local> proximosDe(Coordenada referencia, double raioMetros);
}
