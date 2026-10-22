package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

/** A origem da requisição já atingiu o limite de relatos de obstáculo por hora. */
public class LimiteDeRelatosExcedidoException extends RuntimeException {

    public LimiteDeRelatosExcedidoException() {
        super("Limite de relatos de obstáculo por hora atingido para esta origem. Tente novamente mais tarde.");
    }
}
