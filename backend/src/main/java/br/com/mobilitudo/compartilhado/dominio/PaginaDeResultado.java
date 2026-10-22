package br.com.mobilitudo.compartilhado.dominio;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de itens, mais se há conteúdo além dela.
 *
 * @param itens           o conteúdo desta página, já no tamanho pedido
 * @param temProximaPagina se existe pelo menos mais um item além do que coube aqui
 */
public record PaginaDeResultado<T>(List<T> itens, boolean temProximaPagina) {

    public PaginaDeResultado {
        itens = List.copyOf(itens);
    }

    /**
     * Busca a página pedida e descobre se há mais conteúdo além dela, sem um
     * {@code COUNT(*)} sobre a tabela inteira.
     *
     * <p>Quando esta página já veio incompleta (menos itens que o tamanho pedido), não há
     * o que verificar: não existe página seguinte. Só nesse caso de fronteira — página
     * cheia — é que uma segunda consulta, igualmente barata e indexada, busca a página
     * seguinte só para checar se ela tem algo.</p>
     */
    public static <T> PaginaDeResultado<T> buscar(Paginacao paginacao, Function<Paginacao, List<T>> buscador) {
        List<T> itens = buscador.apply(paginacao);
        boolean paginaCheia = itens.size() == paginacao.tamanho();
        boolean temProxima = paginaCheia && !buscador.apply(paginacao.proximaPagina()).isEmpty();
        return new PaginaDeResultado<>(itens, temProxima);
    }
}
