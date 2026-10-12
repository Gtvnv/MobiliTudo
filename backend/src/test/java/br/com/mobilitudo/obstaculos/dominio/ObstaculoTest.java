package br.com.mobilitudo.obstaculos.dominio;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import br.com.mobilitudo.obstaculos.dominio.modelo.TransicaoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObstaculoTest {

    @Test
    @DisplayName("Relato novo nasce pendente e ainda nao desvia rota de ninguem")
    void relatoNasceSemEfeitoSobreRotas() {
        Obstaculo obstaculo = novoRelato();

        assertThat(obstaculo.status()).isEqualTo(StatusValidacao.PENDENTE);
        assertThat(obstaculo.afetaRoteamento()).isFalse();
        assertThat(obstaculo.avaliadoEm()).isEmpty();
    }

    @Test
    @DisplayName("Relato sem autor identificado continua valido, apenas anônimo")
    void aceitaRelatoAnonimo() {
        Obstaculo obstaculo = Obstaculo.reportar(TipoObstaculo.BURACO, Severidade.LEVE,
                new Coordenada(-23.9925, -46.4095), null, null, "   ");

        assertThat(obstaculo.reportadoPor()).isEqualTo("anônimo");
    }

    @Test
    @DisplayName("Validar passa a afetar o roteamento e registra a data da conferencia")
    void validarHabilitaOEfeitoSobreRotas() {
        Obstaculo obstaculo = novoRelato();

        obstaculo.validar();

        assertThat(obstaculo.status()).isEqualTo(StatusValidacao.VALIDADO);
        assertThat(obstaculo.afetaRoteamento()).isTrue();
        assertThat(obstaculo.avaliadoEm()).isPresent();
    }

    @Test
    @DisplayName("Relato rejeitado nunca mais influencia o mapa")
    void rejeitarEncerraORelato() {
        Obstaculo obstaculo = novoRelato();

        obstaculo.rejeitar();

        assertThat(obstaculo.status()).isEqualTo(StatusValidacao.REJEITADO);
        assertThat(obstaculo.afetaRoteamento()).isFalse();
        assertThat(obstaculo.status().encerrado()).isTrue();
    }

    @Test
    @DisplayName("Um relato ja conferido nao volta para a fila de curadoria")
    void naoRevalidaRelatoJaConferido() {
        Obstaculo obstaculo = novoRelato();
        obstaculo.validar();

        assertThatThrownBy(obstaculo::validar).isInstanceOf(TransicaoInvalidaException.class);
        assertThatThrownBy(obstaculo::rejeitar).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("So se resolve a barreira que chegou a ser confirmada")
    void resolverExigeValidacaoPrevia() {
        Obstaculo pendente = novoRelato();
        assertThatThrownBy(pendente::marcarResolvido).isInstanceOf(TransicaoInvalidaException.class);

        Obstaculo rejeitado = novoRelato();
        rejeitado.rejeitar();
        assertThatThrownBy(rejeitado::marcarResolvido).isInstanceOf(TransicaoInvalidaException.class);

        Obstaculo validado = novoRelato();
        validado.validar();
        validado.marcarResolvido();

        assertThat(validado.status()).isEqualTo(StatusValidacao.RESOLVIDO);
        assertThat(validado.afetaRoteamento()).isFalse();
    }

    private static Obstaculo novoRelato() {
        return Obstaculo.reportar(TipoObstaculo.OBRA, Severidade.MODERADA,
                new Coordenada(-23.9925, -46.4085), "Tapume ocupando a calçada",
                "av-3-4", "yasmim");
    }
}
