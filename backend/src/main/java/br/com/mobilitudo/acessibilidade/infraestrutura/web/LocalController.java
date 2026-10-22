package br.com.mobilitudo.acessibilidade.infraestrutura.web;

import br.com.mobilitudo.acessibilidade.aplicacao.ConsultarLocais;
import br.com.mobilitudo.acessibilidade.aplicacao.RegistrarLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.PaginaDeResultado;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.compartilhado.infraestrutura.web.Perfis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locais")
@Tag(name = "Locais", description = "Acervo de pontos mapeados e seus recursos de acessibilidade")
class LocalController {

    /** Raio padrao de uma busca por proximidade: cobre a vizinhanca a pe sem inundar o mapa. */
    private static final double RAIO_PADRAO_METROS = 2_000;

    private final ConsultarLocais consultarLocais;
    private final RegistrarLocal registrarLocal;

    LocalController(ConsultarLocais consultarLocais, RegistrarLocal registrarLocal) {
        this.consultarLocais = consultarLocais;
        this.registrarLocal = registrarLocal;
    }

    @GetMapping
    @Operation(summary = "Lista locais, opcionalmente filtrados por categoria, proximidade e perfil",
            description = "Paginado: use pagina (a partir de 0) e tamanho (máximo 200, padrão 50). "
                    + "O cabeçalho X-Tem-Proxima-Pagina da resposta diz se há mais itens além desta página.")
    ResponseEntity<List<LocalDTO>> listar(@RequestParam(required = false) String categoria,
                                          @RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng,
                                          @RequestParam(required = false) Double raio,
                                          @RequestParam(required = false) String perfil,
                                          @RequestParam(required = false) Integer pagina,
                                          @RequestParam(required = false) Integer tamanho) {
        PerfilMobilidade perfilMobilidade = Perfis.interpretar(perfil);
        Paginacao paginacao = Paginacao.de(pagina, tamanho);

        PaginaDeResultado<Local> encontrados;
        if (lat != null && lng != null) {
            Coordenada referencia = new Coordenada(lat, lng);
            double raioMetros = raio == null ? RAIO_PADRAO_METROS : raio;
            encontrados = perfil == null
                    ? consultarLocais.proximosDe(referencia, raioMetros, paginacao)
                    : consultarLocais.proximosCompativeisCom(referencia, raioMetros, perfilMobilidade, paginacao);
        } else {
            encontrados = consultarLocais.porCategoria(
                    CategoriaLocal.porIdentificador(categoria).orElse(null), paginacao);
        }

        // O filtro por categoria tambem vale para a busca por proximidade. Ele atua sobre
        // a pagina ja trazida do banco, entao pode devolver menos itens que o tamanho
        // pedido quando parte da pagina pertence a outra categoria.
        List<LocalDTO> corpo = encontrados.itens().stream()
                .filter(local -> categoria == null
                        || CategoriaLocal.porIdentificador(categoria)
                        .map(alvo -> alvo == local.categoria())
                        .orElse(true))
                .map(local -> LocalDTO.de(local, perfilMobilidade))
                .toList();

        return ResponseEntity.ok()
                .header("X-Tem-Proxima-Pagina", String.valueOf(encontrados.temProximaPagina()))
                .body(corpo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um local")
    ResponseEntity<LocalDTO> porId(@PathVariable UUID id,
                                   @RequestParam(required = false) String perfil) {
        return consultarLocais.porId(id)
                .map(local -> ResponseEntity.ok(LocalDTO.de(local, Perfis.interpretar(perfil))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo local mapeado")
    LocalDTO cadastrar(@RequestBody @Valid NovoLocalDTO corpo) {
        Local salvo = registrarLocal.executar(corpo.paraComando());
        return LocalDTO.de(salvo, PerfilMobilidade.PADRAO);
    }
}
