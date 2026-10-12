package br.com.mobilitudo.rotas.infraestrutura.web;

import br.com.mobilitudo.compartilhado.infraestrutura.web.Perfis;
import br.com.mobilitudo.rotas.aplicacao.CalcularRotaAcessivel;
import br.com.mobilitudo.rotas.dominio.modelo.Rota;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rotas")
@Tag(name = "Rotas", description = "Cálculo de trajetos que contornam barreiras de acessibilidade")
class RotaController {

    private final CalcularRotaAcessivel calcularRotaAcessivel;

    RotaController(CalcularRotaAcessivel calcularRotaAcessivel) {
        this.calcularRotaAcessivel = calcularRotaAcessivel;
    }

    @PostMapping("/acessivel")
    @Operation(summary = "Traça a rota de menor custo de acessibilidade entre dois pontos",
            description = """
                    Responde 404 com a propriedade "motivo" quando não há trajeto: \
                    ORIGEM_FORA_DA_AREA_MAPEADA, DESTINO_FORA_DA_AREA_MAPEADA ou \
                    SEM_CAMINHO_ACESSIVEL. O último significa que existe ligação física \
                    entre os pontos, mas nenhuma transponível pelo perfil pedido.""")
    RotaDTO calcular(@RequestBody @Valid PedidoDeRotaDTO pedido) {
        Rota rota = calcularRotaAcessivel.executar(
                pedido.origem().paraDominio(),
                pedido.destino().paraDominio(),
                Perfis.interpretar(pedido.perfil()));
        return RotaDTO.de(rota);
    }
}
