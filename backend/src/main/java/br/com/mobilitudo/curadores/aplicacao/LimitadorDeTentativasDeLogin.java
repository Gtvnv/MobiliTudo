package br.com.mobilitudo.curadores.aplicacao;

import br.com.mobilitudo.compartilhado.infraestrutura.seguranca.LimitadorDeTaxa;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limita tentativas de login por origem, reaproveitando {@link LimitadorDeTaxa} — a
 * mesma janela deslizante em memória já usada para conter abuso no relato de
 * obstáculos, aplicada aqui a um risco diferente: senha escolhida por pessoa é alvo de
 * força bruta de um jeito que o antigo token de alta entropia nunca foi.
 */
@Component
public class LimitadorDeTentativasDeLogin {

    private final LimitadorDeTaxa limitador;

    public LimitadorDeTentativasDeLogin(
            @Value("${mobilitudo.rate-limit.login-tentativas-por-hora:10}") int limitePorHora) {
        this.limitador = new LimitadorDeTaxa(limitePorHora, Duration.ofHours(1));
    }

    /** @throws LimiteDeTentativasDeLoginExcedidoException se a origem já esgotou a cota */
    public void exigirDentroDoLimite(HttpServletRequest requisicao) {
        if (!limitador.permitir(requisicao.getRemoteAddr())) {
            throw new LimiteDeTentativasDeLoginExcedidoException();
        }
    }
}
