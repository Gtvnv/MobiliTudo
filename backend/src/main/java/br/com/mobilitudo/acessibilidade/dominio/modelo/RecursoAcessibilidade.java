package br.com.mobilitudo.acessibilidade.dominio.modelo;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Recurso de acessibilidade presente em um local.
 *
 * <p>Os rotulos sao os mesmos exibidos nos circulos da ficha do local no front-end, o que
 * permite ao mapa continuar lendo os dados vindos da API sem nenhuma tabela de conversao.</p>
 */
public enum RecursoAcessibilidade {

    RAMPA("Rampas", "♿"),
    ELEVADOR("Elevador", "🛗"),
    PISO_TATIL("Piso Tátil", "👣"),
    PLACA_BRAILLE("Placas Braille", "⠃"),
    BANHEIRO_ADAPTADO("Banheiro Adaptado", "🚻"),
    VAGA_PCD("Vaga PCD", "🅿️"),
    CADEIRA_ANFIBIA("Cadeira Anfíbia", "🏖️"),
    CORRIMAO("Corrimão", "🤝"),
    ATENDIMENTO_PRIORITARIO("Atendimento Prioritário", "⏱️"),
    INTERPRETE_LIBRAS("Intérprete de Libras", "🤟");

    private final String rotulo;
    private final String icone;

    RecursoAcessibilidade(String rotulo, String icone) {
        this.rotulo = rotulo;
        this.icone = icone;
    }

    public String rotulo() {
        return rotulo;
    }

    public String icone() {
        return icone;
    }

    /** Aceita o nome da constante ou o rotulo ja usado pelo mapa ("Piso Tátil"). */
    public static Optional<RecursoAcessibilidade> porRotuloOuNome(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values())
                .filter(recurso -> recurso.rotulo.toLowerCase(Locale.ROOT).equals(normalizado)
                        || recurso.name().toLowerCase(Locale.ROOT).equals(normalizado))
                .findFirst();
    }
}
