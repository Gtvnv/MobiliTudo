package br.com.mobilitudo.obstaculos.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

/**
 * O token apresentado não confere com o relato, já foi usado, ou o relato nunca teve
 * um (semeado diretamente no banco, por exemplo).
 *
 * <p>Os três casos respondem da mesma forma de propósito: diferenciar "token errado"
 * de "token já usado" de "este relato não tem token" devolveria a um atacante
 * informação sobre o estado interno do relato que ele não deveria ter.</p>
 */
public class TokenDeExclusaoInvalidoException extends ExcecaoDeDominio {

    public TokenDeExclusaoInvalidoException() {
        super("Token de exclusão inválido, já utilizado, ou inexistente para este relato");
    }
}
