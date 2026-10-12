package br.com.mobilitudo.compartilhado.dominio;

/**
 * Página pedida numa listagem: qual bloco de resultados e de que tamanho.
 *
 * @param pagina  índice da página, a partir de 0
 * @param tamanho quantos itens por página
 */
public record Paginacao(int pagina, int tamanho) {

    public static final int TAMANHO_PADRAO = 50;
    public static final int TAMANHO_MAXIMO = 200;

    public Paginacao {
        if (pagina < 0) {
            throw new IllegalArgumentException("Página não pode ser negativa: " + pagina);
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "Tamanho de página deve estar entre 1 e " + TAMANHO_MAXIMO + ": " + tamanho);
        }
    }

    public static Paginacao de(Integer pagina, Integer tamanho) {
        return new Paginacao(
                pagina == null ? 0 : pagina,
                tamanho == null ? TAMANHO_PADRAO : tamanho);
    }

    /** Quantos registros pular antes do início desta página: página × tamanho. */
    public long deslocamento() {
        return (long) pagina * tamanho;
    }

    /**
     * A página seguinte, do mesmo tamanho.
     *
     * <p>Usada só para checar se há conteúdo além da página atual (ver
     * {@link PaginaDeResultado#buscar}). Pedir a página inteira em vez de só um item é de
     * propósito: {@code offset = página × tamanho} é como o Spring Data (e o SQL por trás)
     * calculam o deslocamento, e qualquer tentativa de pedir "só mais um item" sem mexer no
     * tamanho da página desalinha essa conta — foi exatamente esse desalinhamento que
     * quebrou a primeira versão desta classe.</p>
     */
    public Paginacao proximaPagina() {
        return new Paginacao(pagina + 1, tamanho);
    }
}
