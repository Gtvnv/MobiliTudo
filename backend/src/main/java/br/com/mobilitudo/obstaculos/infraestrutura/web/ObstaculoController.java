package br.com.mobilitudo.obstaculos.infraestrutura.web;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.obstaculos.aplicacao.GerenciarObstaculos;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    ObstaculoController(GerenciarObstaculos gerenciarObstaculos) {
        this.gerenciarObstaculos = gerenciarObstaculos;
    }

    @GetMapping
    @Operation(summary = "Lista obstáculos por status ou por proximidade")
    List<ObstaculoDTO> listar(@RequestParam(required = false) StatusValidacao status,
                              @RequestParam(required = false) Double lat,
                              @RequestParam(required = false) Double lng,
                              @RequestParam(required = false) Double raio) {
        if (lat != null && lng != null) {
            double raioMetros = raio == null ? RAIO_PADRAO_METROS : raio;
            return gerenciarObstaculos.naArea(new Coordenada(lat, lng), raioMetros).stream()
                    .filter(obstaculo -> status == null || obstaculo.status() == status)
                    .map(ObstaculoDTO::de)
                    .toList();
        }
        StatusValidacao alvo = status == null ? StatusValidacao.VALIDADO : status;
        return gerenciarObstaculos.porStatus(alvo).stream().map(ObstaculoDTO::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reporta uma nova barreira",
            description = "O relato nasce PENDENTE e só passa a desviar rotas depois de validado.")
    ObstaculoDTO reportar(@RequestBody @Valid NovoObstaculoDTO corpo) {
        return ObstaculoDTO.de(gerenciarObstaculos.reportar(corpo.paraComando()));
    }

    @PostMapping("/{id}/validacao")
    @Operation(summary = "Confere um relato: VALIDAR, REJEITAR ou RESOLVER",
            description = "Responde 409 quando a transição não é permitida pelo ciclo de vida.")
    ObstaculoDTO avaliar(@PathVariable UUID id, @RequestBody @Valid AvaliacaoDTO corpo) {
        return ObstaculoDTO.de(switch (corpo.acao()) {
            case VALIDAR -> gerenciarObstaculos.validar(id);
            case REJEITAR -> gerenciarObstaculos.rejeitar(id);
            case RESOLVER -> gerenciarObstaculos.marcarResolvido(id);
        });
    }
}
