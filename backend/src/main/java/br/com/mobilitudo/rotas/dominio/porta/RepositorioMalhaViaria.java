package br.com.mobilitudo.rotas.dominio.porta;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.rotas.dominio.modelo.GrafoVias;

/**
 * Porta de saida que entrega a malha viaria ja montada como grafo.
 *
 * <p>Carrega apenas o recorte que interessa ao par origem/destino. Puxar a cidade inteira
 * para cada requisicao funcionaria com os poucos quarteiroes mapeados hoje e deixaria de
 * funcionar no dia em que o mapeamento crescer.</p>
 */
public interface RepositorioMalhaViaria {

    /**
     * Grafo cobrindo o retangulo que contem origem e destino, dilatado pela margem.
     *
     * <p>A margem existe porque o desvio acessivel quase sempre sai da reta entre os dois
     * pontos: sem folga, o caminho alternativo ficaria fora do recorte carregado.</p>
     */
    GrafoVias carregarNaAreaEntre(Coordenada origem, Coordenada destino, double margemMetros);
}
