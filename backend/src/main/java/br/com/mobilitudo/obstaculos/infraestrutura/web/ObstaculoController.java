package br.com.mobilitudo.obstaculos.infraestrutura.web;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.PaginaDeResultado;
import br.com.mobilitudo.compartilhado.dominio.Paginacao;
import br.com.mobilitudo.compartilhado.infraestrutura.seguranca.AutenticadorDeCurador;
import br.com.mobilitudo.compartilhado.infraestrutura.seguranca.LimitadorDeRelatosDeObstaculo;
import br.com.mobilitudo.obstaculos.aplicacao.GerenciarObstaculos;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/obstaculos")
@Tag(name = "Obstáculos", description = "Relato colaborativo e curadoria de barreiras urbanas")
class ObstaculoController {

    private static final double RAIO_PADRAO_METROS = 2_000;

    private final GerenciarObstaculos gerenciarObstaculos;
    private final AutenticadorDeCurador autenticadorDeCurador;
    private final LimitadorDeRelatosDeObstaculo limitadorDeRelatos;

    ObstaculoController(GerenciarObstaculos gerenciarObstaculos,
                        AutenticadorDeCurador autenticadorDeCurador,
                        LimitadorDeRelatosDeObstaculo limitadorDeRelatos) {
        this.gerenciarObstaculos = gerenciarObstaculos;
        this.autenticadorDeCurador = autenticadorDeCurador;
        this.limitadorDeRelatos = limitadorDeRelatos;
    }

    @GetMapping
    @Operation(summary = "Lista obstáculos por status ou por proximidade",
            description = "Paginado: use pagina (a partir de 0) e tamanho (máximo 200, padrão 50). "
                    + "O cabeçalho X-Tem-Proxima-Pagina da resposta diz se há mais itens além desta página.")
    ResponseEntity<List<ObstaculoDTO>> listar(@RequestParam(required = false) StatusValidacao status,
                                              @RequestParam(required = false) Double lat,
                                              @RequestParam(required = false) Double lng,
                                              @RequestParam(required = false) Double raio,
                                              @RequestParam(required = false) Integer pagina,
                                              @RequestParam(required = false) Integer tamanho) {
        Paginacao paginacao = Paginacao.de(pagina, tamanho);
        PaginaDeResultado<Obstaculo> encontrados;

        if (lat != null && lng != null) {
            double raioMetros = raio == null ? RAIO_PADRAO_METROS : raio;
            encontrados = gerenciarObstaculos.naArea(new Coordenada(lat, lng), raioMetros, paginacao);
        } else {
            encontrados = gerenciarObstaculos.porStatus(
                    status == null ? StatusValidacao.VALIDADO : status, paginacao);
        }

        // Quando a busca e por proximidade, o filtro de status (se pedido) atua sobre a
        // pagina ja trazida do banco — assim como em locais, pode devolver menos itens
        // que o tamanho pedido.
        List<ObstaculoDTO> corpo = encontrados.itens().stream()
                .filter(obstaculo -> lat == null || lng == null || status == null || obstaculo.status() == status)
                .map(ObstaculoDTO::de)
                .toList();

        return ResponseEntity.ok()
                .header("X-Tem-Proxima-Pagina", String.valueOf(encontrados.temProximaPagina()))
                .body(corpo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reporta uma nova barreira",
            description = "O relato nasce PENDENTE e só passa a desviar rotas depois de validado. A resposta "
                    + "traz tokenExclusao uma única vez — guarde-o se quiser remover seu nome do relato "
                    + "depois (ver POST /{id}/anonimizar); ele não pode ser recuperado se for perdido. "
                    + "Responde 429 acima do limite de relatos por hora da origem.")
    ObstaculoDTO reportar(@RequestBody @Valid NovoObstaculoDTO corpo, HttpServletRequest requisicao) {
        limitadorDeRelatos.exigirDentroDoLimite(requisicao);
        GerenciarObstaculos.ObstaculoReportado resultado = gerenciarObstaculos.reportar(corpo.paraComando());
        return ObstaculoDTO.deComToken(resultado.obstaculo(), resultado.tokenExclusao());
    }

    @PostMapping("/{id}/anonimizar")
    @Operation(summary = "Remove o nome de quem relatou, mediante o token de exclusão recebido na criação",
            description = "Autoexclusão de dado pessoal (LGPD art. 18): não exige o token de curadoria, "
                    + "só a posse do tokenExclusao devolvido no POST original. Token errado, já usado, ou "
                    + "relato sem token associado respondem 403, sem distinguir qual dos três casos é — "
                    + "ver docs/privacidade.md.")
    ObstaculoDTO anonimizar(@PathVariable UUID id, @RequestBody @Valid AnonimizarObstaculoDTO corpo) {
        return ObstaculoDTO.de(gerenciarObstaculos.anonimizar(id, corpo.tokenExclusao()));
    }

    @PostMapping("/{id}/validacao")
    @Operation(summary = "Confere um relato: VALIDAR, REJEITAR ou RESOLVER",
            description = "Exige o cabeçalho X-Mobilitudo-Curador-Token (ver AutenticadorDeCurador). "
                    + "Responde 401 sem o token correto, 503 se a curadoria não estiver configurada no "
                    + "servidor, e 409 quando a transição não é permitida pelo ciclo de vida.")
    ObstaculoDTO avaliar(@PathVariable UUID id, @RequestBody @Valid AvaliacaoDTO corpo, HttpServletRequest requisicao) {
        autenticadorDeCurador.exigirTokenValido(requisicao);
        return ObstaculoDTO.de(switch (corpo.acao()) {
            case VALIDAR -> gerenciarObstaculos.validar(id);
            case REJEITAR -> gerenciarObstaculos.rejeitar(id);
            case RESOLVER -> gerenciarObstaculos.marcarResolvido(id);
        });
    }
}
