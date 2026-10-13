package br.com.mobilitudo.curadores.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;

/**
 * E-mail não cadastrado, senha errada, conta desativada, ou token de sessão
 * inválido/expirado.
 *
 * <p>Todos esses casos respondem com a mesma mensagem genérica, de propósito: dizer
 * "este e-mail não existe" diferente de "a senha está errada" permite a quem ataca
 * descobrir, tentativa por tentativa, quais e-mails têm conta cadastrada — um
 * vazamento de informação pequeno, mas real e evitável. O mesmo vale para "esta conta
 * foi desativada": isso também não precisa ser dito a quem está tentando entrar com uma
 * senha que pode nem ser a certa.</p>
 */
public class CredenciaisInvalidasException extends ExcecaoDeDominio {

    public CredenciaisInvalidasException() {
        super("E-mail, senha ou sessão inválidos.");
    }
}
