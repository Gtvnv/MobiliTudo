package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;

import java.util.Objects;

/**
 * Vertice da malha viaria: um cruzamento, uma esquina ou o ponto de acesso de um local.
 *
 * @param id         identificador estavel do no na malha
 * @param coordenada posicao geografica do no
 */
public record NoVia(String id, Coordenada coordenada) {

    public NoVia {
        Objects.requireNonNull(id, "id do no e obrigatorio");
        Objects.requireNonNull(coordenada, "coordenada do no e obrigatoria");
    }
}
