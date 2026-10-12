package br.com.mobilitudo.compartilhado.infraestrutura.web;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.RecursoAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Vocabulario do dominio exposto para o front-end.
 *
 * <p>Evita que os seletores de perfil, categoria e tipo de obstaculo sejam listas fixas
 * copiadas no JavaScript: acrescentar um recurso de acessibilidade no enum passa a
 * aparecer no formulario sem tocar no mapa.</p>
 */
@RestController
@RequestMapping("/api/v1/metadados")
@Tag(name = "Metadados", description = "Vocabulário do domínio para montar os formulários do mapa")
class MetadadosController {

    @GetMapping
    @Operation(summary = "Perfis, categorias, recursos, tipos de obstáculo e severidades")
    Map<String, Object> listar() {
        return Map.of(
                "perfis", Arrays.stream(PerfilMobilidade.values())
                        .map(perfil -> Map.of(
                                "id", perfil.name(),
                                "rotulo", perfil.descricao(),
                                "inclinacaoMaximaPercentual", perfil.inclinacaoMaximaPercentual(),
                                "larguraMinimaMetros", perfil.larguraMinimaMetros()))
                        .toList(),
                "categorias", Arrays.stream(CategoriaLocal.values())
                        .map(categoria -> Map.of(
                                "id", categoria.identificador(),
                                "rotulo", categoria.rotulo()))
                        .toList(),
                "recursos", Arrays.stream(RecursoAcessibilidade.values())
                        .map(recurso -> Map.of(
                                "id", recurso.name(),
                                "rotulo", recurso.rotulo(),
                                "icone", recurso.icone()))
                        .toList(),
                "tiposDeObstaculo", Arrays.stream(TipoObstaculo.values())
                        .map(tipo -> Map.of(
                                "id", tipo.name(),
                                "rotulo", tipo.descricao(),
                                "bloqueiaCadeiraDeRodas", tipo.intransponivelParaCadeirante()))
                        .toList(),
                "severidades", severidades());
    }

    private static List<Map<String, Object>> severidades() {
        return Arrays.stream(Severidade.values())
                .map(severidade -> Map.<String, Object>of(
                        "id", severidade.name(),
                        "peso", severidade.peso()))
                .toList();
    }
}
