package br.com.mobilitudo.compartilhado.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Converte a {@link Paginacao} do domínio para o {@link Pageable} que o Spring Data
 * entende.
 *
 * <p>Fica na infraestrutura pelo mesmo motivo de {@code Geometrias}: o domínio não deve
 * conhecer tipos do Spring.</p>
 */
public final class PaginacaoJpa {

    private PaginacaoJpa() {
    }

    public static Pageable semOrdenacao(Paginacao paginacao) {
        return PageRequest.of(paginacao.pagina(), paginacao.tamanho());
    }

    public static Pageable ordenadaPor(Paginacao paginacao, Sort ordenacao) {
        return PageRequest.of(paginacao.pagina(), paginacao.tamanho(), ordenacao);
    }
}
