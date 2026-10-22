package br.com.mobilitudo.acessibilidade.dominio.modelo;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tipo de ponto mapeado.
 *
 * <p>Os identificadores minusculos ({@code shopping}, {@code praia}, ...) sao os mesmos
 * ja usados pelos filtros do mapa no front-end, mantidos para nao quebrar o que existe.</p>
 */
public enum CategoriaLocal {

    SHOPPING("shopping", "Shoppings"),
    PRAIA("praia", "Praias"),
    TRANSPORTE("transporte", "Terminais e transporte"),
    CULTURA("cultura", "Cultura e lazer"),
    SAUDE("saude", "Saúde"),
    EDUCACAO("educacao", "Educação"),
    SERVICO_PUBLICO("servico-publico", "Serviços públicos"),
    OUTRO("outro", "Outros");

    private final String identificador;
    private final String rotulo;

    CategoriaLocal(String identificador, String rotulo) {
        this.identificador = identificador;
        this.rotulo = rotulo;
    }

    public String identificador() {
        return identificador;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Aceita tanto o identificador do front-end quanto o nome da constante. */
    public static Optional<CategoriaLocal> porIdentificador(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values())
                .filter(categoria -> categoria.identificador.equals(normalizado)
                        || categoria.name().toLowerCase(Locale.ROOT).equals(normalizado))
                .findFirst();
    }
}
