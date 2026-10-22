package br.com.mobilitudo.curadores.aplicacao;

/**
 * Muitas tentativas de login a partir da mesma origem em pouco tempo.
 *
 * <p>Diferente do antigo token de curadoria (um segredo de alta entropia, impraticável
 * de adivinhar mesmo sem limite de tentativas), uma senha escolhida por uma pessoa é
 * comparativamente muito mais sujeita a força bruta ou ataque de dicionário — é essa
 * diferença de risco que a conta individual introduz, e que este limite existe para
 * conter.</p>
 */
public class LimiteDeTentativasDeLoginExcedidoException extends RuntimeException {

    public LimiteDeTentativasDeLoginExcedidoException() {
        super("Muitas tentativas de login em pouco tempo. Tente novamente mais tarde.");
    }
}
