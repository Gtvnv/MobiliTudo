package br.com.mobilitudo.acessibilidade.infraestrutura.web;

import br.com.mobilitudo.acessibilidade.aplicacao.RegistrarLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.RecursoAcessibilidade;
import br.com.mobilitudo.compartilhado.infraestrutura.web.CoordenadaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Entrada do cadastro colaborativo de um novo local. */
public record NovoLocalDTO(
        @NotBlank @Size(max = 160) String nome,
        String categoria,
        @NotNull @Valid CoordenadaDTO coordenada,
        @Size(max = 255) String endereco,
        String descricao,
        @Size(max = 512) String imagemUrl,
        List<String> recursos) {

    public RegistrarLocal.Comando paraComando() {
        return new RegistrarLocal.Comando(
                nome,
                CategoriaLocal.porIdentificador(categoria).orElse(CategoriaLocal.OUTRO),
                coordenada.paraDominio(),
                endereco,
                descricao,
                imagemUrl,
                recursosReconhecidos());
    }

    /**
     * Converte os rotulos recebidos, ignorando em silencio o que nao for reconhecido.
     *
     * <p>Recusar o cadastro inteiro por causa de um recurso escrito de forma diferente
     * desperdicaria a contribuicao de quem foi ate o local levantar a informacao.</p>
     */
    private Set<RecursoAcessibilidade> recursosReconhecidos() {
        if (recursos == null) {
            return Set.of();
        }
        return recursos.stream()
                .map(RecursoAcessibilidade::porRotuloOuNome)
                .flatMap(java.util.Optional::stream)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
