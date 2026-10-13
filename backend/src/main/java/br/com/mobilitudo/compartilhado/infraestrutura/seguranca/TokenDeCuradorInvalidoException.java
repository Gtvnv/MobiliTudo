package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

/** Cabeçalho de curadoria ausente ou com valor incorreto. */
public class TokenDeCuradorInvalidoException extends RuntimeException {

    public TokenDeCuradorInvalidoException() {
        super("Cabeçalho " + AutenticadorDeCurador.CABECALHO_TOKEN + " ausente ou inválido.");
    }
}
