package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.ExcecaoDeDominio;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;

/**
 * Nao existe caminho acessivel entre os pontos pedidos para o perfil informado.
 *
 * <p>O motivo e parte da resposta: "voce esta fora da area mapeada" e "existe caminho,
 * mas nenhum que uma cadeira de rodas vence" sao situacoes diferentes para quem pediu
 * a rota, e so a segunda sinaliza uma barreira urbana a ser reportada.</p>
 */
public class RotaNaoEncontradaException extends ExcecaoDeDominio {

    private final Motivo motivo;

    public RotaNaoEncontradaException(Motivo motivo, String mensagem) {
        super(mensagem);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }

    public static RotaNaoEncontradaException origemForaDaArea(double raioMetros) {
        return new RotaNaoEncontradaException(Motivo.ORIGEM_FORA_DA_AREA_MAPEADA,
                "Nenhum ponto da malha viária mapeada a menos de %.0f m da origem".formatted(raioMetros));
    }

    public static RotaNaoEncontradaException destinoForaDaArea(double raioMetros) {
        return new RotaNaoEncontradaException(Motivo.DESTINO_FORA_DA_AREA_MAPEADA,
                "Nenhum ponto da malha viária mapeada a menos de %.0f m do destino".formatted(raioMetros));
    }

    public static RotaNaoEncontradaException semCaminhoAcessivel(PerfilMobilidade perfil) {
        return new RotaNaoEncontradaException(Motivo.SEM_CAMINHO_ACESSIVEL,
                "Existe ligação entre os pontos, mas nenhuma transponível para o perfil "
                        + perfil.descricao());
    }

    public enum Motivo {
        ORIGEM_FORA_DA_AREA_MAPEADA,
        DESTINO_FORA_DA_AREA_MAPEADA,
        SEM_CAMINHO_ACESSIVEL
    }
}
