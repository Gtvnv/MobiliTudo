package br.com.mobilitudo.acessibilidade.infraestrutura.web;

import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.modelo.RecursoAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representacao de um local na API.
 *
 * <p>O campo {@code coords} e a lista {@code recursos} de rotulos reproduzem o formato do
 * antigo {@code locais.json}, para que o mapa consuma a API sem reescrever a renderizacao
 * da ficha do local.</p>
 */
@Schema(name = "Local", description = "Ponto mapeado com seus recursos de acessibilidade")
public record LocalDTO(UUID id,
                       String nome,
                       String categoria,
                       List<Double> coords,
                       String endereco,
                       String info,
                       String imagem,
                       List<String> recursos,
                       String nivelAcessibilidade,
                       String nivelDescricao,
                       Instant atualizadoEm) {

    public static LocalDTO de(Local local, PerfilMobilidade perfil) {
        var nivel = local.nivelPara(perfil);
        return new LocalDTO(
                local.id(),
                local.nome(),
                local.categoria().identificador(),
                List.of(local.coordenada().latitude(), local.coordenada().longitude()),
                local.endereco(),
                local.descricao(),
                local.imagemUrl(),
                local.recursos().stream().map(RecursoAcessibilidade::rotulo).sorted().toList(),
                nivel.name(),
                nivel.descricao(),
                local.atualizadoEm());
    }
}
