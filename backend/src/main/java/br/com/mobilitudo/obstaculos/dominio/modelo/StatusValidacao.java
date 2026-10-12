package br.com.mobilitudo.obstaculos.dominio.modelo;

/**
 * Ciclo de vida de um obstaculo reportado pela comunidade.
 *
 * <p>Apenas {@link #VALIDADO} influencia o calculo de rotas. Um relato ainda nao conferido
 * nao pode bloquear o trajeto de outra pessoa, e um ja resolvido nao deve continuar
 * penalizando uma calcada que foi consertada.</p>
 */
public enum StatusValidacao {

    PENDENTE,
    VALIDADO,
    REJEITADO,
    RESOLVIDO;

    public boolean afetaRoteamento() {
        return this == VALIDADO;
    }

    public boolean encerrado() {
        return this == REJEITADO || this == RESOLVIDO;
    }
}
