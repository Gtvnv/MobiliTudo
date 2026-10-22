package br.com.mobilitudo.compartilhado.dominio;

/**
 * Barreira fisica mapeada sobre a via.
 *
 * <p>{@code intransponivelParaCadeirante} marca os tipos que nenhuma cadeira de rodas
 * vence, independentemente da severidade reportada: eles eliminam o trecho do grafo
 * em vez de apenas encarece-lo.</p>
 */
public enum TipoObstaculo {

    DEGRAU("Degrau sem rampa", true),
    ESCADA("Escada sem alternativa acessível", true),
    SEM_CALCADA("Trecho sem calçada", true),
    SEM_REBAIXAMENTO_GUIA("Travessia sem rebaixamento de guia", true),
    BURACO("Buraco ou piso danificado", false),
    OBRA("Obra ou interdição na calçada", false),
    POSTE_MAL_POSICIONADO("Poste ou mobiliário obstruindo a faixa livre", false),
    VEGETACAO("Vegetação invadindo a calçada", false),
    VEICULO_SOBRE_CALCADA("Veículo estacionado sobre a calçada", false),
    PISO_ESCORREGADIO("Piso escorregadio", false),
    AUSENCIA_PISO_TATIL("Ausência de piso tátil em ponto crítico", false);

    private final String descricao;
    private final boolean intransponivelParaCadeirante;

    TipoObstaculo(String descricao, boolean intransponivelParaCadeirante) {
        this.descricao = descricao;
        this.intransponivelParaCadeirante = intransponivelParaCadeirante;
    }

    public String descricao() {
        return descricao;
    }

    public boolean intransponivelParaCadeirante() {
        return intransponivelParaCadeirante;
    }
}
