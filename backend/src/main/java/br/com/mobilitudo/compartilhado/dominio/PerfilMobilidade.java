package br.com.mobilitudo.compartilhado.dominio;


/**
 * Necessidades de mobilidade do usuario que pede a rota.
 *
 * <p>E o perfil, e nao o trecho, que decide o que e intransponivel: a mesma calcada
 * pode ser plenamente acessivel para quem anda com bengala e inviavel para uma
 * cadeira de rodas. Os parametros seguem a ABNT NBR 9050:2020 - faixa livre minima
 * de 1,20 m e inclinacao longitudinal de rampa de ate 8,33%.</p>
 */
public enum PerfilMobilidade {

    CADEIRA_DE_RODAS(
            "Cadeira de rodas", true,
            8.33, 5.0,
            1.20, 1.50,
            true, true, false,
            Severidade.MODERADA,
            0.80),

    MOBILIDADE_REDUZIDA(
            "Mobilidade reduzida", false,
            12.0, 8.0,
            0.90, 1.20,
            true, false, false,
            Severidade.CRITICA,
            0.70),

    DEFICIENCIA_VISUAL(
            "Deficiência visual", false,
            20.0, 15.0,
            0.80, 1.20,
            false, false, true,
            Severidade.CRITICA,
            0.90),

    PADRAO(
            "Sem restrições", false,
            30.0, 20.0,
            0.60, 1.20,
            false, false, false,
            Severidade.CRITICA,
            1.30);

    /** Desconto aplicado ao custo quando o trecho tem piso tatil e o perfil se beneficia dele. */
    public static final double FATOR_PISO_TATIL = 0.85;

    private final String descricao;
    private final boolean sobreRodas;
    private final double inclinacaoMaximaPercentual;
    private final double inclinacaoConfortavelPercentual;
    private final double larguraMinimaMetros;
    private final double larguraConfortavelMetros;
    private final boolean exigeSuperficieFirme;
    private final boolean exigeRebaixamentoDeGuia;
    private final boolean beneficiaDePisoTatil;
    private final Severidade severidadeBloqueante;
    private final double velocidadeMediaMetrosPorSegundo;

    PerfilMobilidade(String descricao,
                     boolean sobreRodas,
                     double inclinacaoMaximaPercentual,
                     double inclinacaoConfortavelPercentual,
                     double larguraMinimaMetros,
                     double larguraConfortavelMetros,
                     boolean exigeSuperficieFirme,
                     boolean exigeRebaixamentoDeGuia,
                     boolean beneficiaDePisoTatil,
                     Severidade severidadeBloqueante,
                     double velocidadeMediaMetrosPorSegundo) {
        this.descricao = descricao;
        this.sobreRodas = sobreRodas;
        this.inclinacaoMaximaPercentual = inclinacaoMaximaPercentual;
        this.inclinacaoConfortavelPercentual = inclinacaoConfortavelPercentual;
        this.larguraMinimaMetros = larguraMinimaMetros;
        this.larguraConfortavelMetros = larguraConfortavelMetros;
        this.exigeSuperficieFirme = exigeSuperficieFirme;
        this.exigeRebaixamentoDeGuia = exigeRebaixamentoDeGuia;
        this.beneficiaDePisoTatil = beneficiaDePisoTatil;
        this.severidadeBloqueante = severidadeBloqueante;
        this.velocidadeMediaMetrosPorSegundo = velocidadeMediaMetrosPorSegundo;
    }

    public String descricao() {
        return descricao;
    }

    /**
     * Perfil que se desloca sobre rodas.
     *
     * <p>Separado da inclinacao e da largura porque degraus e escadas nao sao questao
     * de grau: sao barreiras absolutas para quem usa cadeira de rodas, e apenas
     * dificuldades para quem caminha.</p>
     */
    public boolean sobreRodas() {
        return sobreRodas;
    }

    public double inclinacaoMaximaPercentual() {
        return inclinacaoMaximaPercentual;
    }

    public double inclinacaoConfortavelPercentual() {
        return inclinacaoConfortavelPercentual;
    }

    public double larguraMinimaMetros() {
        return larguraMinimaMetros;
    }

    public double larguraConfortavelMetros() {
        return larguraConfortavelMetros;
    }

    public boolean exigeSuperficieFirme() {
        return exigeSuperficieFirme;
    }

    public boolean exigeRebaixamentoDeGuia() {
        return exigeRebaixamentoDeGuia;
    }

    public boolean beneficiaDePisoTatil() {
        return beneficiaDePisoTatil;
    }

    public Severidade severidadeBloqueante() {
        return severidadeBloqueante;
    }

    public double velocidadeMediaMetrosPorSegundo() {
        return velocidadeMediaMetrosPorSegundo;
    }

    /**
     * Menor multiplicador de custo que um trecho pode receber neste perfil.
     *
     * <p>A heuristica do A* (distancia em linha reta) precisa ser multiplicada por este
     * valor para continuar admissivel. Sem isso, o bonus de piso tatil tornaria a
     * heuristica otimista demais e o algoritmo poderia devolver uma rota pior que a
     * otima.</p>
     */
    public double fatorMinimoDeCusto() {
        return beneficiaDePisoTatil ? FATOR_PISO_TATIL : 1.0;
    }
}
