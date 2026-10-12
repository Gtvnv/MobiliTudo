package br.com.mobilitudo.rotas.aplicacao;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;
import br.com.mobilitudo.rotas.dominio.modelo.GrafoVias;
import br.com.mobilitudo.rotas.dominio.modelo.Rota;
import br.com.mobilitudo.rotas.dominio.modelo.RotaNaoEncontradaException;
import br.com.mobilitudo.rotas.dominio.porta.RepositorioMalhaViaria;
import br.com.mobilitudo.rotas.dominio.servico.CalculadoraRotaAcessivel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: tracar a rota acessivel entre dois pontos.
 *
 * <p>A camada de aplicacao so orquestra — carrega o recorte da malha e entrega ao dominio.
 * Nenhuma regra de acessibilidade mora aqui, e e por isso que o algoritmo pode ser testado
 * sem subir Spring nem banco.</p>
 */
@Service
public class CalcularRotaAcessivel {

    /**
     * Folga adicionada ao retangulo origem/destino ao carregar a malha.
     *
     * <p>600 m cobrem com sobra o desvio de um quarteirao tipico de Praia Grande, que e a
     * escala em que os contornos de barreira acontecem.</p>
     */
    private static final double MARGEM_DE_BUSCA_METROS = 600;

    private final RepositorioMalhaViaria repositorioMalhaViaria;
    private final CalculadoraRotaAcessivel calculadora;

    public CalcularRotaAcessivel(RepositorioMalhaViaria repositorioMalhaViaria,
                                 CalculadoraRotaAcessivel calculadora) {
        this.repositorioMalhaViaria = repositorioMalhaViaria;
        this.calculadora = calculadora;
    }

    /**
     * @throws RotaNaoEncontradaException quando nao ha caminho transponivel para o perfil
     */
    @Transactional(readOnly = true)
    public Rota executar(Coordenada origem, Coordenada destino, PerfilMobilidade perfil) {
        GrafoVias malha = repositorioMalhaViaria.carregarNaAreaEntre(origem, destino, MARGEM_DE_BUSCA_METROS);
        if (malha.vazio()) {
            throw RotaNaoEncontradaException.origemForaDaArea(CalculadoraRotaAcessivel.RAIO_DE_APROXIMACAO_METROS);
        }
        return calculadora.calcular(malha, origem, destino, perfil);
    }
}
