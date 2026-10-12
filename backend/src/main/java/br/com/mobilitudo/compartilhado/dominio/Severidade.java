package br.com.mobilitudo.compartilhado.dominio;

/**
 * Gravidade com que um obstaculo afeta a travessia de um trecho.
 *
 * <p>O {@code peso} entra no custo do trecho; a ordem das constantes define o que
 * cada perfil considera bloqueante (ver {@code PerfilMobilidade.severidadeBloqueante()}).</p>
 */
public enum Severidade {

    LEVE(0.2),
    MODERADA(0.6),
    CRITICA(1.5);

    private final double peso;

    Severidade(double peso) {
        this.peso = peso;
    }

    public double peso() {
        return peso;
    }

    public boolean peloMenos(Severidade minima) {
        return this.ordinal() >= minima.ordinal();
    }
}
