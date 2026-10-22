package br.com.mobilitudo.compartilhado.infraestrutura.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Janela deslizante em memória: no máximo {@code limite} eventos por chave a cada hora.
 *
 * <p>Existe para que reportar um obstáculo continue aberto a qualquer pessoa — sem exigir
 * cadastro, que afastaria exatamente quem o projeto quer ouvir — sem que isso signifique
 * que uma única origem possa inundar o mapa de relatos falsos ou sobrecarregar a fila de
 * curadoria. Em memória e por instância é a escolha certa para o volume e para a única
 * réplica deste projeto; se a API rodar com várias instâncias atrás de um balanceador, o
 * limite deixa de ser global e passa a valer por instância — nesse ponto o substituto é
 * um contador compartilhado (Redis), não esta classe.</p>
 */
public class LimitadorDeTaxa {

    private final int limite;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> eventosPorChave = new ConcurrentHashMap<>();

    public LimitadorDeTaxa(int limitePorJanela, Duration janela) {
        this(limitePorJanela, janela, Clock.systemUTC());
    }

    LimitadorDeTaxa(int limitePorJanela, Duration janela, Clock relogio) {
        this.limite = limitePorJanela;
        this.janela = janela;
        this.relogio = relogio;
    }

    /**
     * Registra uma tentativa para a chave e informa se ela ainda cabe no limite.
     *
     * <p>Thread-safe mesmo sob concorrência na mesma chave: a fila é de acesso
     * concorrente, e cada chamada remove o que já saiu da janela antes de contar.</p>
     */
    public boolean permitir(String chave) {
        Instant agora = relogio.instant();
        Instant corte = agora.minus(janela);

        Deque<Instant> eventos = eventosPorChave.computeIfAbsent(chave, k -> new ConcurrentLinkedDeque<>());
        while (true) {
            Instant maisAntigo = eventos.peekFirst();
            if (maisAntigo == null || maisAntigo.isAfter(corte)) {
                break;
            }
            eventos.pollFirst();
        }

        if (eventos.size() >= limite) {
            return false;
        }
        eventos.addLast(agora);
        return true;
    }
}
