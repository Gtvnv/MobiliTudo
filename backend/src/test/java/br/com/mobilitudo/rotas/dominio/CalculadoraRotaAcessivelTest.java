package br.com.mobilitudo.rotas.dominio;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.rotas.dominio.modelo.GrafoVias;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;
import br.com.mobilitudo.rotas.dominio.modelo.PassoRota;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.rotas.dominio.modelo.Rota;
import br.com.mobilitudo.rotas.dominio.modelo.RotaNaoEncontradaException;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;
import br.com.mobilitudo.rotas.dominio.servico.CalculadoraRotaAcessivel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.DESTINO;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.ESQUINA_NORTE;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.ESQUINA_SUL;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.NORDESTE;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.ORIGEM;
import static br.com.mobilitudo.rotas.dominio.MalhaDeTeste.calcadaBoa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraRotaAcessivelTest {

    private static final ObstaculoNoTrecho DEGRAU =
            new ObstaculoNoTrecho(TipoObstaculo.DEGRAU, Severidade.MODERADA);

    private final CalculadoraRotaAcessivel calculadora = new CalculadoraRotaAcessivel();

    @Test
    @DisplayName("Sem barreiras, a rota escolhida e a mais curta")
    void escolheOCaminhoCurtoQuandoTudoEAcessivel() {
        Rota rota = calculadora.calcular(malhaComDesvio(false), ORIGEM.coordenada(), DESTINO.coordenada(),
                PerfilMobilidade.PADRAO);

        assertThat(idsDosTrechos(rota)).containsExactly("direto");
        assertThat(rota.nivelGeral()).isEqualTo(NivelAcessibilidade.PLENO);
    }

    @Test
    @DisplayName("Com degrau no caminho curto, a cadeira de rodas e desviada pelo trajeto mais longo e sem barreira")
    void desviaDaBarreiraParaCadeiraDeRodas() {
        GrafoVias malha = malhaComDesvio(true);

        Rota rotaAcessivel = calculadora.calcular(malha, ORIGEM.coordenada(), DESTINO.coordenada(),
                PerfilMobilidade.CADEIRA_DE_RODAS);
        Rota rotaCurta = calculadora.calcular(malha, ORIGEM.coordenada(), DESTINO.coordenada(),
                PerfilMobilidade.PADRAO);

        assertThat(idsDosTrechos(rotaAcessivel)).containsExactly("norte-1", "norte-2");
        assertThat(idsDosTrechos(rotaCurta)).containsExactly("direto");

        // A tese do projeto: a rota acessivel e mais longa em metros e menor em barreiras.
        assertThat(rotaAcessivel.distanciaTotalMetros()).isGreaterThan(rotaCurta.distanciaTotalMetros());
        assertThat(rotaAcessivel.nivelGeral()).isEqualTo(NivelAcessibilidade.PLENO);
    }

    @Test
    @DisplayName("Quando todos os caminhos tem barreira, a resposta e a ausencia de rota, nunca uma rota inviavel")
    void naoInventaRotaQuandoTodoCaminhoEstaBloqueado() {
        List<SegmentoVia> todosComDegrau = List.of(
                calcadaBoa("direto", ORIGEM, DESTINO).obstaculos(List.of(DEGRAU)).construir(),
                calcadaBoa("norte-1", ORIGEM, ESQUINA_NORTE).obstaculos(List.of(DEGRAU)).construir(),
                calcadaBoa("norte-2", ESQUINA_NORTE, DESTINO).obstaculos(List.of(DEGRAU)).construir());

        assertThatThrownBy(() -> calculadora.calcular(GrafoVias.de(todosComDegrau),
                ORIGEM.coordenada(), DESTINO.coordenada(), PerfilMobilidade.CADEIRA_DE_RODAS))
                .isInstanceOf(RotaNaoEncontradaException.class)
                .extracting(excecao -> ((RotaNaoEncontradaException) excecao).motivo())
                .isEqualTo(RotaNaoEncontradaException.Motivo.SEM_CAMINHO_ACESSIVEL);
    }

    @Test
    @DisplayName("Ponto fora da area mapeada e distinguido de ausencia de caminho acessivel")
    void informaQuandoOPontoEstaForaDaAreaMapeada() {
        Coordenada saoPaulo = new Coordenada(-23.5505, -46.6333);

        assertThatThrownBy(() -> calculadora.calcular(malhaComDesvio(false), saoPaulo, DESTINO.coordenada(),
                PerfilMobilidade.PADRAO))
                .isInstanceOf(RotaNaoEncontradaException.class)
                .extracting(excecao -> ((RotaNaoEncontradaException) excecao).motivo())
                .isEqualTo(RotaNaoEncontradaException.Motivo.ORIGEM_FORA_DA_AREA_MAPEADA);
    }

    @Test
    @DisplayName("Entre dois caminhos de mesma extensao, o perfil com deficiencia visual segue o piso tatil")
    void perfilVisualPrefereOCaminhoComPisoTatil() {
        GrafoVias malha = GrafoVias.de(List.of(
                calcadaBoa("norte-1", ORIGEM, ESQUINA_NORTE).possuiPisoTatil(true).construir(),
                calcadaBoa("norte-2", ESQUINA_NORTE, DESTINO).possuiPisoTatil(true).construir(),
                calcadaBoa("sul-1", ORIGEM, ESQUINA_SUL).construir(),
                calcadaBoa("sul-2", ESQUINA_SUL, DESTINO).construir()));

        Rota rota = calculadora.calcular(malha, ORIGEM.coordenada(), DESTINO.coordenada(),
                PerfilMobilidade.DEFICIENCIA_VISUAL);

        assertThat(idsDosTrechos(rota)).containsExactly("norte-1", "norte-2");
    }

    @Test
    @DisplayName("As instrucoes descrevem as manobras e fecham no destino")
    void instrucoesDescrevemAsManobras() {
        GrafoVias malha = GrafoVias.de(List.of(
                calcadaBoa("norte-1", ORIGEM, ESQUINA_NORTE).nomeVia("Avenida Presidente Kennedy").construir(),
                calcadaBoa("norte-leste", ESQUINA_NORTE, NORDESTE).nomeVia("Rua Xixova").construir()));

        Rota rota = calculadora.calcular(malha, ORIGEM.coordenada(), NORDESTE.coordenada(),
                PerfilMobilidade.PADRAO);

        assertThat(rota.passos()).hasSize(2);
        assertThat(rota.passos().getFirst().instrucao())
                .startsWith("Siga por Avenida Presidente Kennedy");
        // Giro de 90 graus para leste: a manobra e uma curva a direita, nao uma curva acentuada.
        assertThat(rota.passos().getLast().instrucao())
                .contains("Vire à direita em Rua Xixova")
                .endsWith("Você chegou ao destino.");
    }

    @Test
    @DisplayName("A rota reporta distancia, custo ponderado e duracao coerentes com o perfil")
    void reportaDistanciaCustoEDuracao() {
        Rota rota = calculadora.calcular(malhaComDesvio(false), ORIGEM.coordenada(), DESTINO.coordenada(),
                PerfilMobilidade.CADEIRA_DE_RODAS);

        assertThat(rota.distanciaTotalMetros()).isCloseTo(203, org.assertj.core.data.Offset.offset(5.0));
        // Concreto plano e largo: custo ponderado igual a distancia, sem penalidade alguma.
        assertThat(rota.custoPonderado()).isCloseTo(rota.distanciaTotalMetros(),
                org.assertj.core.data.Offset.offset(0.01));
        assertThat(rota.duracaoEstimadaMinutos()).isCloseTo(
                rota.distanciaTotalMetros() / PerfilMobilidade.CADEIRA_DE_RODAS.velocidadeMediaMetrosPorSegundo() / 60,
                org.assertj.core.data.Offset.offset(0.01));
        assertThat(rota.geometria()).hasSize(2);
    }

    @Test
    @DisplayName("Trechos com ressalva aparecem nos avisos da rota")
    void rotaConsolidaAsRessalvasDosTrechos() {
        GrafoVias malha = GrafoVias.de(List.of(
                calcadaBoa("direto", ORIGEM, DESTINO).inclinacaoPercentual(7.0).construir()));

        Rota rota = calculadora.calcular(malha, ORIGEM.coordenada(), DESTINO.coordenada(),
                PerfilMobilidade.CADEIRA_DE_RODAS);

        assertThat(rota.nivelGeral()).isEqualTo(NivelAcessibilidade.COM_RESSALVAS);
        assertThat(rota.avisos()).anySatisfy(aviso -> assertThat(aviso).startsWith("Subida de 7"));
    }

    @Test
    @DisplayName("Origem e destino no mesmo ponto nao produzem trajeto")
    void origemIgualAoDestino() {
        Rota rota = calculadora.calcular(malhaComDesvio(false), ORIGEM.coordenada(), ORIGEM.coordenada(),
                PerfilMobilidade.PADRAO);

        assertThat(rota.distanciaTotalMetros()).isZero();
        assertThat(rota.passos()).singleElement()
                .extracting(PassoRota::instrucao)
                .isEqualTo("Você já está no destino.");
    }

    /** Caminho curto direto mais um desvio pelo norte; o direto pode ou nao ter um degrau. */
    private GrafoVias malhaComDesvio(boolean degrauNoCaminhoDireto) {
        SegmentoVia.Construtor direto = calcadaBoa("direto", ORIGEM, DESTINO);
        if (degrauNoCaminhoDireto) {
            direto.obstaculos(List.of(DEGRAU));
        }
        return GrafoVias.de(List.of(
                direto.construir(),
                calcadaBoa("norte-1", ORIGEM, ESQUINA_NORTE).construir(),
                calcadaBoa("norte-2", ESQUINA_NORTE, DESTINO).construir()));
    }

    private static List<String> idsDosTrechos(Rota rota) {
        return rota.passos().stream().map(PassoRota::segmentoId).toList();
    }
}
