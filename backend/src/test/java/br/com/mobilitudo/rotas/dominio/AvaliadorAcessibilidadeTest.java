package br.com.mobilitudo.rotas.dominio;

import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;
import br.com.mobilitudo.rotas.dominio.modelo.TipoSuperficie;
import br.com.mobilitudo.rotas.dominio.servico.AvaliadorAcessibilidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.DESTINO;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.ORIGEM;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.calcadaBoa;
import static org.assertj.core.api.Assertions.assertThat;

class AvaliadorAcessibilidadeTest {

    private final AvaliadorAcessibilidade avaliador = new AvaliadorAcessibilidade();

    @Test
    @DisplayName("Degrau e barreira absoluta para quem usa cadeira de rodas, e apenas custo para quem caminha")
    void degrauBloqueiaApenasPerfisSobreRodas() {
        SegmentoVia comDegrau = calcadaBoa("t1", ORIGEM, DESTINO)
                .obstaculos(List.of(new ObstaculoNoTrecho(TipoObstaculo.DEGRAU, Severidade.LEVE)))
                .construir();

        assertThat(avaliador.transitavel(comDegrau, PerfilMobilidade.CADEIRA_DE_RODAS)).isFalse();
        assertThat(avaliador.transitavel(comDegrau, PerfilMobilidade.MOBILIDADE_REDUZIDA)).isTrue();
        assertThat(avaliador.transitavel(comDegrau, PerfilMobilidade.PADRAO)).isTrue();
    }

    @Test
    @DisplayName("Obstaculo critico bloqueia qualquer perfil")
    void obstaculoCriticoBloqueiaTodosOsPerfis() {
        SegmentoVia interditado = calcadaBoa("t1", ORIGEM, DESTINO)
                .obstaculos(List.of(new ObstaculoNoTrecho(TipoObstaculo.OBRA, Severidade.CRITICA)))
                .construir();

        for (PerfilMobilidade perfil : PerfilMobilidade.values()) {
            assertThat(avaliador.transitavel(interditado, perfil))
                    .as("perfil %s", perfil)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("Faixa livre abaixo de 1,20 m inviabiliza a cadeira de rodas (NBR 9050)")
    void faixaLivreEstreitaBloqueiaCadeiraDeRodas() {
        SegmentoVia estreita = calcadaBoa("t1", ORIGEM, DESTINO).larguraMetros(0.95).construir();

        assertThat(avaliador.transitavel(estreita, PerfilMobilidade.CADEIRA_DE_RODAS)).isFalse();
        assertThat(avaliador.transitavel(estreita, PerfilMobilidade.MOBILIDADE_REDUZIDA)).isTrue();
    }

    @Test
    @DisplayName("Rampa acima de 8,33% inviabiliza a cadeira de rodas")
    void inclinacaoAcimaDoLimiteBloqueia() {
        SegmentoVia rampaIngreme = calcadaBoa("t1", ORIGEM, DESTINO).inclinacaoPercentual(12.0).construir();
        SegmentoVia rampaSuave = calcadaBoa("t2", ORIGEM, DESTINO).inclinacaoPercentual(6.0).construir();

        assertThat(avaliador.transitavel(rampaIngreme, PerfilMobilidade.CADEIRA_DE_RODAS)).isFalse();
        assertThat(avaliador.transitavel(rampaSuave, PerfilMobilidade.CADEIRA_DE_RODAS)).isTrue();
    }

    @Test
    @DisplayName("Descida ingreme pesa tanto quanto a subida equivalente")
    void inclinacaoEAvaliadaEmModulo() {
        SegmentoVia subida = calcadaBoa("t1", ORIGEM, DESTINO).inclinacaoPercentual(7.0).construir();
        SegmentoVia descida = calcadaBoa("t2", ORIGEM, DESTINO).inclinacaoPercentual(-7.0).construir();

        assertThat(avaliador.fatorDeCusto(descida, PerfilMobilidade.CADEIRA_DE_RODAS))
                .isEqualTo(avaliador.fatorDeCusto(subida, PerfilMobilidade.CADEIRA_DE_RODAS));
    }

    @Test
    @DisplayName("Areia derruba perfis que exigem superficie firme, mas nao quem nao tem restricao")
    void superficieInstavelBloqueiaQuemExigeFirmeza() {
        SegmentoVia areia = calcadaBoa("t1", ORIGEM, DESTINO).superficie(TipoSuperficie.AREIA).construir();

        assertThat(avaliador.transitavel(areia, PerfilMobilidade.CADEIRA_DE_RODAS)).isFalse();
        assertThat(avaliador.transitavel(areia, PerfilMobilidade.MOBILIDADE_REDUZIDA)).isFalse();
        assertThat(avaliador.transitavel(areia, PerfilMobilidade.PADRAO)).isTrue();
    }

    @Test
    @DisplayName("Travessia sem rebaixamento de guia bloqueia a cadeira de rodas e onera os demais")
    void travessiaSemRebaixamento() {
        SegmentoVia travessia = calcadaBoa("t1", ORIGEM, DESTINO)
                .travessia(true)
                .possuiRebaixamentoDeGuia(false)
                .construir();
        SegmentoVia travessiaRebaixada = calcadaBoa("t2", ORIGEM, DESTINO)
                .travessia(true)
                .possuiRebaixamentoDeGuia(true)
                .construir();

        assertThat(avaliador.transitavel(travessia, PerfilMobilidade.CADEIRA_DE_RODAS)).isFalse();
        assertThat(avaliador.transitavel(travessia, PerfilMobilidade.MOBILIDADE_REDUZIDA)).isTrue();
        assertThat(avaliador.fatorDeCusto(travessia, PerfilMobilidade.MOBILIDADE_REDUZIDA))
                .isGreaterThan(avaliador.fatorDeCusto(travessiaRebaixada, PerfilMobilidade.MOBILIDADE_REDUZIDA));
    }

    @Test
    @DisplayName("Piso tatil barateia o trecho para quem tem deficiencia visual e e indiferente para os demais")
    void pisoTatilBeneficiaApenasQuemUsa() {
        SegmentoVia comPisoTatil = calcadaBoa("t1", ORIGEM, DESTINO).possuiPisoTatil(true).construir();
        SegmentoVia semPisoTatil = calcadaBoa("t2", ORIGEM, DESTINO).possuiPisoTatil(false).construir();

        assertThat(avaliador.fatorDeCusto(comPisoTatil, PerfilMobilidade.DEFICIENCIA_VISUAL))
                .isLessThan(avaliador.fatorDeCusto(semPisoTatil, PerfilMobilidade.DEFICIENCIA_VISUAL));
        assertThat(avaliador.fatorDeCusto(comPisoTatil, PerfilMobilidade.CADEIRA_DE_RODAS))
                .isEqualTo(avaliador.fatorDeCusto(semPisoTatil, PerfilMobilidade.CADEIRA_DE_RODAS));
    }

    @ParameterizedTest
    @EnumSource(PerfilMobilidade.class)
    @DisplayName("Nenhum trecho custa menos que o fator minimo do perfil, que e o que mantem o A* correto")
    void fatorDeCustoRespeitaOPisoDoPerfil(PerfilMobilidade perfil) {
        SegmentoVia melhorTrechoPossivel = calcadaBoa("t1", ORIGEM, DESTINO)
                .superficie(TipoSuperficie.ASFALTO)
                .larguraMetros(4.0)
                .possuiPisoTatil(true)
                .construir();

        assertThat(avaliador.fatorDeCusto(melhorTrechoPossivel, perfil))
                .isGreaterThanOrEqualTo(perfil.fatorMinimoDeCusto());
    }

    @Test
    @DisplayName("Trecho sem nenhuma ressalva e classificado como plenamente acessivel")
    void nivelPlenoQuandoNaoHaRessalvas() {
        SegmentoVia ideal = calcadaBoa("t1", ORIGEM, DESTINO).superficie(TipoSuperficie.CONCRETO).construir();

        assertThat(avaliador.ressalvas(ideal, PerfilMobilidade.CADEIRA_DE_RODAS)).isEmpty();
        assertThat(avaliador.nivel(ideal, PerfilMobilidade.CADEIRA_DE_RODAS))
                .isEqualTo(NivelAcessibilidade.PLENO);
    }

    @Test
    @DisplayName("Ressalvas descrevem a barreira em linguagem apresentavel ao usuario")
    void ressalvasDescrevemAsBarreiras() {
        SegmentoVia trecho = calcadaBoa("t1", ORIGEM, DESTINO)
                .inclinacaoPercentual(9.0)
                .larguraMetros(1.3)
                .iluminacaoAdequada(false)
                .construir();

        assertThat(avaliador.ressalvas(trecho, PerfilMobilidade.MOBILIDADE_REDUZIDA))
                .anySatisfy(ressalva -> assertThat(ressalva).startsWith("Subida de"))
                .anySatisfy(ressalva -> assertThat(ressalva).contains("Iluminação deficiente"));
        assertThat(avaliador.nivel(trecho, PerfilMobilidade.MOBILIDADE_REDUZIDA))
                .isEqualTo(NivelAcessibilidade.COM_RESSALVAS);
    }
}
