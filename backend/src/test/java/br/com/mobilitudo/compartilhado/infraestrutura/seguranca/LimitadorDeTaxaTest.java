package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorDeTaxaTest {

    @Test
    @DisplayName("Permite até o limite e barra a partir daí, na mesma janela")
    void permiteAteOLimite() {
        var relogio = new RelogioControlavel(Instant.parse("2026-01-01T10:00:00Z"));
        var limitador = new LimitadorDeTaxa(3, Duration.ofHours(1), relogio);

        assertThat(limitador.permitir("1.2.3.4")).isTrue();
        assertThat(limitador.permitir("1.2.3.4")).isTrue();
        assertThat(limitador.permitir("1.2.3.4")).isTrue();
        assertThat(limitador.permitir("1.2.3.4")).isFalse();
    }

    @Test
    @DisplayName("Cada chave tem a própria cota")
    void chavesSaoIndependentes() {
        var relogio = new RelogioControlavel(Instant.parse("2026-01-01T10:00:00Z"));
        var limitador = new LimitadorDeTaxa(1, Duration.ofHours(1), relogio);

        assertThat(limitador.permitir("1.2.3.4")).isTrue();
        assertThat(limitador.permitir("1.2.3.4")).isFalse();
        assertThat(limitador.permitir("5.6.7.8")).isTrue();
    }

    @Test
    @DisplayName("Eventos fora da janela deslizante não contam mais para o limite")
    void janelaDeslizanteLiberaComOTempo() {
        var relogio = new RelogioControlavel(Instant.parse("2026-01-01T10:00:00Z"));
        var limitador = new LimitadorDeTaxa(2, Duration.ofHours(1), relogio);

        assertThat(limitador.permitir("1.2.3.4")).isTrue();
        assertThat(limitador.permitir("1.2.3.4")).isTrue();
        assertThat(limitador.permitir("1.2.3.4")).isFalse();

        relogio.avancar(Duration.ofMinutes(61));

        assertThat(limitador.permitir("1.2.3.4")).isTrue();
    }

    /** Relógio mutável para testar a janela deslizante sem depender do tempo real. */
    private static final class RelogioControlavel extends Clock {
        private Instant agora;

        RelogioControlavel(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
