package br.com.mobilitudo.rotas.dominio.servico;

import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Traduz os atributos fisicos de um trecho no custo que ele representa para um perfil.
 *
 * <p>Concentra toda a regra de acessibilidade do projeto em um unico lugar, livre de
 * framework: o algoritmo de rota apenas pergunta "da para passar?" e "quanto custa?".
 * Trocar a politica de pesos nao deve exigir tocar no A*.</p>
 *
 * <p>Invariante que o roteador depende: {@link #fatorDeCusto} nunca devolve menos que
 * {@link PerfilMobilidade#fatorMinimoDeCusto()}. E o que mantem a heuristica do A*
 * admissivel.</p>
 */
public class AvaliadorAcessibilidade {

    /** Acrescimo de custo por ponto percentual de inclinacao acima do confortavel. */
    private static final double PESO_INCLINACAO = 0.20;

    /** Acrescimo maximo por faixa livre estreita, aplicado quando a largura chega ao minimo. */
    private static final double PESO_LARGURA = 0.35;

    /** Travessia de rua sempre carrega risco de conflito com veiculos. */
    private static final double PESO_TRAVESSIA = 0.15;

    /** Travessia sem guia rebaixada, para perfis que conseguem transpor o desnivel. */
    private static final double PESO_SEM_REBAIXAMENTO = 0.50;

    private static final double PESO_ILUMINACAO_RUIM = 0.10;

    /**
     * Indica se o trecho pode ser percorrido por este perfil.
     *
     * <p>Um trecho intransponivel some do grafo: nao basta encarece-lo, porque um caminho
     * caro ainda seria sugerido quando nao houver alternativa, e sugerir uma escada para
     * uma cadeira de rodas e pior do que admitir que nao ha rota.</p>
     */
    public boolean transitavel(SegmentoVia trecho, PerfilMobilidade perfil) {
        for (ObstaculoNoTrecho obstaculo : trecho.obstaculos()) {
            if (perfil.sobreRodas() && obstaculo.tipo().intransponivelParaCadeirante()) {
                return false;
            }
            if (obstaculo.severidade().peloMenos(perfil.severidadeBloqueante())) {
                return false;
            }
        }
        if (trecho.larguraMetros() < perfil.larguraMinimaMetros()) {
            return false;
        }
        if (trecho.inclinacaoAbsolutaPercentual() > perfil.inclinacaoMaximaPercentual()) {
            return false;
        }
        if (perfil.exigeSuperficieFirme() && !trecho.superficie().firmeEstavel()) {
            return false;
        }
        return !(perfil.exigeRebaixamentoDeGuia() && trecho.travessia() && !trecho.possuiRebaixamentoDeGuia());
    }

    /**
     * Multiplicador aplicado a distancia do trecho.
     *
     * <p>1.0 e um trecho ideal. 2.0 significa que vencer 100 m ali custa o mesmo que
     * vencer 200 m de calcada plana e larga.</p>
     */
    public double fatorDeCusto(SegmentoVia trecho, PerfilMobilidade perfil) {
        double fator = trecho.superficie().fatorDificuldade();

        double inclinacaoExcedente =
                Math.max(0, trecho.inclinacaoAbsolutaPercentual() - perfil.inclinacaoConfortavelPercentual());
        fator += inclinacaoExcedente * PESO_INCLINACAO;

        fator += penalidadePorLargura(trecho, perfil);

        for (ObstaculoNoTrecho obstaculo : trecho.obstaculos()) {
            fator += obstaculo.severidade().peso();
        }

        if (trecho.travessia()) {
            fator += PESO_TRAVESSIA;
            if (!trecho.possuiRebaixamentoDeGuia()) {
                fator += PESO_SEM_REBAIXAMENTO;
            }
        }
        if (!trecho.iluminacaoAdequada()) {
            fator += PESO_ILUMINACAO_RUIM;
        }
        if (trecho.possuiPisoTatil() && perfil.beneficiaDePisoTatil()) {
            fator *= PerfilMobilidade.FATOR_PISO_TATIL;
        }
        return fator;
    }

    public double custo(SegmentoVia trecho, PerfilMobilidade perfil) {
        return trecho.comprimentoMetros() * fatorDeCusto(trecho, perfil);
    }

    /** Pontos de atencao do trecho, em linguagem apresentavel ao usuario final. */
    public List<String> ressalvas(SegmentoVia trecho, PerfilMobilidade perfil) {
        List<String> ressalvas = new ArrayList<>();

        if (trecho.inclinacaoAbsolutaPercentual() > perfil.inclinacaoConfortavelPercentual()) {
            String sentido = trecho.inclinacaoPercentual() > 0 ? "Subida" : "Descida";
            ressalvas.add("%s de %s%% neste trecho"
                    .formatted(sentido, formatar(trecho.inclinacaoAbsolutaPercentual())));
        }
        if (trecho.larguraMetros() < perfil.larguraConfortavelMetros()) {
            ressalvas.add("Faixa livre estreita (%s m)".formatted(formatar(trecho.larguraMetros())));
        }
        if (!trecho.superficie().firmeEstavel()) {
            ressalvas.add("Piso de %s, sem firmeza garantida"
                    .formatted(trecho.superficie().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        }
        if (trecho.travessia() && !trecho.possuiRebaixamentoDeGuia()) {
            ressalvas.add("Travessia sem rebaixamento de guia");
        }
        if (!trecho.iluminacaoAdequada()) {
            ressalvas.add("Iluminação deficiente");
        }
        if (perfil.beneficiaDePisoTatil() && !trecho.possuiPisoTatil()) {
            ressalvas.add("Trecho sem piso tátil");
        }
        for (ObstaculoNoTrecho obstaculo : trecho.obstaculos()) {
            ressalvas.add("%s (severidade %s)".formatted(
                    obstaculo.tipo().descricao(),
                    obstaculo.severidade().name().toLowerCase(Locale.ROOT)));
        }
        return List.copyOf(ressalvas);
    }

    public NivelAcessibilidade nivel(SegmentoVia trecho, PerfilMobilidade perfil) {
        if (!transitavel(trecho, perfil)) {
            return NivelAcessibilidade.INACESSIVEL;
        }
        return ressalvas(trecho, perfil).isEmpty()
                ? NivelAcessibilidade.PLENO
                : NivelAcessibilidade.COM_RESSALVAS;
    }

    private double penalidadePorLargura(SegmentoVia trecho, PerfilMobilidade perfil) {
        double confortavel = perfil.larguraConfortavelMetros();
        double minima = perfil.larguraMinimaMetros();
        if (trecho.larguraMetros() >= confortavel || confortavel <= minima) {
            return 0;
        }
        double estreitamento = (confortavel - trecho.larguraMetros()) / (confortavel - minima);
        return PESO_LARGURA * Math.min(1.0, estreitamento);
    }

    private static String formatar(double valor) {
        return String.format(Locale.of("pt", "BR"), "%.2f", valor).replaceAll("0+$", "").replaceAll(",$", "");
    }
}
