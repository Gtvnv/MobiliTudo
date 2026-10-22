package br.com.mobilitudo.compartilhado.dominio;

/**
 * Classificacao de acessibilidade usada tanto por locais quanto por trechos de rota.
 *
 * <p>A ordem das constantes vai do melhor para o pior cenario: {@link #pior(NivelAcessibilidade)}
 * depende disso para consolidar o nivel geral de uma rota a partir dos seus trechos.</p>
 */
public enum NivelAcessibilidade {

    /** Atende integralmente aos parametros do perfil (referencia: ABNT NBR 9050). */
    PLENO("Acessível"),

    /** Transitavel, porem com trechos que exigem atencao ou esforco adicional. */
    COM_RESSALVAS("Acessível com ressalvas"),

    /** Possui barreira intransponivel para o perfil avaliado. */
    INACESSIVEL("Inacessível");

    private final String descricao;

    NivelAcessibilidade(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public NivelAcessibilidade pior(NivelAcessibilidade outro) {
        return this.ordinal() >= outro.ordinal() ? this : outro;
    }
}
