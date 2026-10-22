package br.com.mobilitudo.compartilhado.dominio;

/**
 * Raiz das excecoes de regra de negocio.
 *
 * <p>Sao traduzidas para respostas HTTP pelo manipulador global de erros; o dominio
 * em si nao conhece status code algum.</p>
 */
public abstract class ExcecaoDeDominio extends RuntimeException {

    protected ExcecaoDeDominio(String mensagem) {
        super(mensagem);
    }
}
