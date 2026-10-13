package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limita quantos relatos de obstáculo uma mesma origem pode enviar por hora.
 *
 * <p>Protege só {@code POST /api/v1/obstaculos} (o relato em si). Listar continua sem
 * limite — é leitura — e a curadoria já exige token, então não precisa deste limite por
 * cima.</p>
 *
 * <p>Chamado diretamente de dentro do controller, pelo mesmo raciocínio documentado em
 * {@link AutenticadorDeCurador}: um filtro decidindo a partir de {@code getRequestURI()}
 * pode divergir de como o Spring MVC realmente roteou a requisição. Aqui a consequência
 * de uma divergência seria menor (abuso de volume, não um desvio de autorização
 * completo), mas o conserto é igualmente barato e evita manter duas fontes de verdade.</p>
 *
 * <p>A chave de limite é {@link HttpServletRequest#getRemoteAddr()}, o endereço TCP de
 * quem conectou direto. Atrás de um proxy reverso que não preserve ou declare o IP
 * original, isso conta o proxy, não o cliente — correto para a topologia atual (API
 * exposta direto); se um proxy confiável entrar no caminho, a chave precisa passar a ler
 * {@code X-Forwarded-For} apenas dele, nunca de qualquer requisição, ou o próprio limite
 * vira trivialmente contornável.</p>
 */
@Component
public class LimitadorDeRelatosDeObstaculo {

    private final LimitadorDeTaxa limitador;

    public LimitadorDeRelatosDeObstaculo(@Value("${mobilitudo.rate-limit.obstaculos-por-hora:20}") int limitePorHora) {
        this.limitador = new LimitadorDeTaxa(limitePorHora, Duration.ofHours(1));
    }

    /** @throws LimiteDeRelatosExcedidoException se a origem já esgotou a cota da hora */
    public void exigirDentroDoLimite(HttpServletRequest requisicao) {
        if (!limitador.permitir(requisicao.getRemoteAddr())) {
            throw new LimiteDeRelatosExcedidoException();
        }
    }
}
