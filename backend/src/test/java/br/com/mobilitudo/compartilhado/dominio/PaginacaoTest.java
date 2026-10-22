package br.com.mobilitudo.compartilhado.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginacaoTest {

    @Test
    @DisplayName("Página e tamanho ausentes caem nos padrões")
    void valoresPadrao() {
        Paginacao paginacao = Paginacao.de(null, null);

        assertThat(paginacao.pagina()).isZero();
        assertThat(paginacao.tamanho()).isEqualTo(Paginacao.TAMANHO_PADRAO);
    }

    @Test
    @DisplayName("O deslocamento é página vezes tamanho")
    void deslocamento() {
        assertThat(new Paginacao(0, 50).deslocamento()).isZero();
        assertThat(new Paginacao(2, 50).deslocamento()).isEqualTo(100);
    }

    @Test
    @DisplayName("Página negativa é recusada")
    void recusaPaginaNegativa() {
        assertThatThrownBy(() -> new Paginacao(-1, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Tamanho fora de [1, 200] é recusado")
    void recusaTamanhoForaDoIntervalo() {
        assertThatThrownBy(() -> new Paginacao(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Paginacao(0, 300)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("proximaPagina preserva o tamanho, só avança o índice da página")
    void proximaPaginaPreservaOTamanho() {
        Paginacao atual = new Paginacao(3, 20);

        assertThat(atual.proximaPagina()).isEqualTo(new Paginacao(4, 20));
    }

    @Test
    @DisplayName("PaginaDeResultado.buscar detecta a próxima página na primeira página")
    void detectaProximaPaginaNaPrimeiraPagina() {
        List<Integer> universo = List.of(1, 2, 3, 4, 5);

        var pagina = PaginaDeResultado.buscar(new Paginacao(0, 2), p -> fatiar(universo, p));

        assertThat(pagina.itens()).containsExactly(1, 2);
        assertThat(pagina.temProximaPagina()).isTrue();
    }

    @Test
    @DisplayName("PaginaDeResultado.buscar não acusa próxima página na última")
    void naoAcusaProximaPaginaNaUltima() {
        List<Integer> universo = List.of(1, 2, 3, 4, 5);

        var pagina = PaginaDeResultado.buscar(new Paginacao(2, 2), p -> fatiar(universo, p));

        assertThat(pagina.itens()).containsExactly(5);
        assertThat(pagina.temProximaPagina()).isFalse();
    }

    @Test
    @DisplayName("Regressão: a página do meio usa o deslocamento correto, não o de outra página")
    void paginaDoMeioUsaODeslocamentoCorreto() {
        // Universo de 100 itens, páginas de 10: a página de índice 5 (a sexta) precisa
        // começar no item 51, não em qualquer outro ponto calculado a partir de um
        // tamanho de página diferente do pedido.
        List<Integer> universo = java.util.stream.IntStream.rangeClosed(1, 100).boxed().toList();

        var pagina = PaginaDeResultado.buscar(new Paginacao(5, 10), p -> fatiar(universo, p));

        assertThat(pagina.itens()).containsExactly(51, 52, 53, 54, 55, 56, 57, 58, 59, 60);
        assertThat(pagina.temProximaPagina()).isTrue();
    }

    /** Simula o que um repositório faria: aplicar deslocamento e tamanho sobre uma lista. */
    private static List<Integer> fatiar(List<Integer> universo, Paginacao paginacao) {
        int inicio = Math.min(universo.size(), (int) paginacao.deslocamento());
        int fim = Math.min(universo.size(), inicio + paginacao.tamanho());
        return universo.subList(inicio, fim);
    }
}
