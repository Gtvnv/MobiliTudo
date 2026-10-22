package br.com.mobilitudo.rotas.dominio.modelo;

/**
 * Revestimento do piso de um trecho de calcada ou travessia.
 *
 * <p>O {@code fatorDificuldade} multiplica o custo do trecho: 1.0 e um piso ideal,
 * valores maiores representam superficies que exigem mais esforco para vencer a
 * mesma distancia.</p>
 */
public enum TipoSuperficie {

    ASFALTO(1.0, true),
    CONCRETO(1.0, true),
    PISO_INTERTRAVADO(1.1, true),
    LADRILHO_HIDRAULICO(1.15, true),
    PARALELEPIPEDO(1.8, true),
    TERRA_COMPACTADA(2.2, false),
    GRAMA(2.6, false),
    AREIA(4.0, false),
    DESCONHECIDA(1.4, false);

    private final double fatorDificuldade;
    private final boolean firmeEstavel;

    TipoSuperficie(double fatorDificuldade, boolean firmeEstavel) {
        this.fatorDificuldade = fatorDificuldade;
        this.firmeEstavel = firmeEstavel;
    }

    public double fatorDificuldade() {
        return fatorDificuldade;
    }

    /** Superficie firme, estavel e antiderrapante nos termos da ABNT NBR 9050, item 6.1.1. */
    public boolean firmeEstavel() {
        return firmeEstavel;
    }
}
