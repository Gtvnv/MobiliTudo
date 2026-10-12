package br.com.mobilitudo.rotas.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;

import java.util.List;
import java.util.Objects;

/**
 * Aresta da malha viaria: um trecho continuo de calcada, travessia ou passeio
 * entre dois nos, com os atributos de acessibilidade levantados em campo.
 *
 * <p>A inclinacao e orientada: positiva quando o trecho sobe de {@code origem} para
 * {@code destino}. Percorrer o mesmo trecho no sentido contrario e mais facil, e e
 * isso que {@link #inverso()} representa.</p>
 */
public final class SegmentoVia {

    private final String id;
    private final String nomeVia;
    private final NoVia origem;
    private final NoVia destino;
    private final double comprimentoMetros;
    private final double inclinacaoPercentual;
    private final double larguraMetros;
    private final TipoSuperficie superficie;
    private final boolean possuiPisoTatil;
    private final boolean possuiRebaixamentoDeGuia;
    private final boolean travessia;
    private final boolean iluminacaoAdequada;
    private final List<ObstaculoNoTrecho> obstaculos;
    private final List<Coordenada> geometria;

    private SegmentoVia(Construtor construtor) {
        this.id = Objects.requireNonNull(construtor.id, "id do segmento e obrigatorio");
        this.nomeVia = construtor.nomeVia == null ? "Trecho sem nome" : construtor.nomeVia;
        this.origem = Objects.requireNonNull(construtor.origem, "origem do segmento e obrigatoria");
        this.destino = Objects.requireNonNull(construtor.destino, "destino do segmento e obrigatorio");
        this.superficie = construtor.superficie == null ? TipoSuperficie.DESCONHECIDA : construtor.superficie;
        this.inclinacaoPercentual = construtor.inclinacaoPercentual;
        this.larguraMetros = construtor.larguraMetros;
        this.possuiPisoTatil = construtor.possuiPisoTatil;
        this.possuiRebaixamentoDeGuia = construtor.possuiRebaixamentoDeGuia;
        this.travessia = construtor.travessia;
        this.iluminacaoAdequada = construtor.iluminacaoAdequada;
        this.obstaculos = List.copyOf(construtor.obstaculos);
        this.geometria = construtor.geometria.isEmpty()
                ? List.of(origem.coordenada(), destino.coordenada())
                : List.copyOf(construtor.geometria);

        if (origem.id().equals(destino.id())) {
            throw new IllegalArgumentException("Segmento " + id + " liga um no a ele mesmo");
        }
        if (larguraMetros <= 0) {
            throw new IllegalArgumentException("Largura do segmento " + id + " deve ser positiva");
        }
        // Quando o comprimento nao vem do levantamento, cai para a distancia geodesica entre os nos.
        this.comprimentoMetros = construtor.comprimentoMetros > 0
                ? construtor.comprimentoMetros
                : origem.coordenada().distanciaEmMetrosAte(destino.coordenada());
    }

    public static Construtor comId(String id) {
        return new Construtor(id);
    }

    /**
     * Mesmo trecho percorrido no sentido oposto.
     *
     * <p>Inverte os nos, a geometria e o sinal da inclinacao; os demais atributos
     * (largura, superficie, obstaculos) nao dependem do sentido.</p>
     *
     * <p>O id e preservado: continua sendo a mesma calcada, e e esse id que o usuario
     * ve ao reportar uma barreira no trecho. O grafo indexa as arestas pelo no de
     * origem, entao os dois sentidos convivem sem conflito.</p>
     */
    public SegmentoVia inverso() {
        return new Construtor(id)
                .nomeVia(nomeVia)
                .entre(destino, origem)
                .comprimentoMetros(comprimentoMetros)
                .inclinacaoPercentual(-inclinacaoPercentual)
                .larguraMetros(larguraMetros)
                .superficie(superficie)
                .possuiPisoTatil(possuiPisoTatil)
                .possuiRebaixamentoDeGuia(possuiRebaixamentoDeGuia)
                .travessia(travessia)
                .iluminacaoAdequada(iluminacaoAdequada)
                .obstaculos(obstaculos)
                .geometria(geometria.reversed())
                .construir();
    }

    public String id() {
        return id;
    }

    public String nomeVia() {
        return nomeVia;
    }

    public NoVia origem() {
        return origem;
    }

    public NoVia destino() {
        return destino;
    }

    public double comprimentoMetros() {
        return comprimentoMetros;
    }

    public double inclinacaoPercentual() {
        return inclinacaoPercentual;
    }

    /** Inclinacao em modulo: uma descida de 10% cansa menos, mas ainda assim e uma rampa. */
    public double inclinacaoAbsolutaPercentual() {
        return Math.abs(inclinacaoPercentual);
    }

    public double larguraMetros() {
        return larguraMetros;
    }

    public TipoSuperficie superficie() {
        return superficie;
    }

    public boolean possuiPisoTatil() {
        return possuiPisoTatil;
    }

    public boolean possuiRebaixamentoDeGuia() {
        return possuiRebaixamentoDeGuia;
    }

    public boolean travessia() {
        return travessia;
    }

    public boolean iluminacaoAdequada() {
        return iluminacaoAdequada;
    }

    public List<ObstaculoNoTrecho> obstaculos() {
        return obstaculos;
    }

    public List<Coordenada> geometria() {
        return geometria;
    }

    @Override
    public String toString() {
        return "SegmentoVia[" + id + " " + origem.id() + "->" + destino.id() + "]";
    }

    /** Construtor fluente: a lista de atributos de um trecho e longa demais para um construtor posicional. */
    public static final class Construtor {

        private final String id;
        private String nomeVia;
        private NoVia origem;
        private NoVia destino;
        private double comprimentoMetros;
        private double inclinacaoPercentual;
        private double larguraMetros = 1.50;
        private TipoSuperficie superficie = TipoSuperficie.DESCONHECIDA;
        private boolean possuiPisoTatil;
        private boolean possuiRebaixamentoDeGuia = true;
        private boolean travessia;
        private boolean iluminacaoAdequada = true;
        private List<ObstaculoNoTrecho> obstaculos = List.of();
        private List<Coordenada> geometria = List.of();

        private Construtor(String id) {
            this.id = id;
        }

        public Construtor nomeVia(String nomeVia) {
            this.nomeVia = nomeVia;
            return this;
        }

        public Construtor entre(NoVia origem, NoVia destino) {
            this.origem = origem;
            this.destino = destino;
            return this;
        }

        public Construtor comprimentoMetros(double comprimentoMetros) {
            this.comprimentoMetros = comprimentoMetros;
            return this;
        }

        public Construtor inclinacaoPercentual(double inclinacaoPercentual) {
            this.inclinacaoPercentual = inclinacaoPercentual;
            return this;
        }

        public Construtor larguraMetros(double larguraMetros) {
            this.larguraMetros = larguraMetros;
            return this;
        }

        public Construtor superficie(TipoSuperficie superficie) {
            this.superficie = superficie;
            return this;
        }

        public Construtor possuiPisoTatil(boolean possuiPisoTatil) {
            this.possuiPisoTatil = possuiPisoTatil;
            return this;
        }

        public Construtor possuiRebaixamentoDeGuia(boolean possuiRebaixamentoDeGuia) {
            this.possuiRebaixamentoDeGuia = possuiRebaixamentoDeGuia;
            return this;
        }

        public Construtor travessia(boolean travessia) {
            this.travessia = travessia;
            return this;
        }

        public Construtor iluminacaoAdequada(boolean iluminacaoAdequada) {
            this.iluminacaoAdequada = iluminacaoAdequada;
            return this;
        }

        public Construtor obstaculos(List<ObstaculoNoTrecho> obstaculos) {
            this.obstaculos = obstaculos == null ? List.of() : obstaculos;
            return this;
        }

        public Construtor geometria(List<Coordenada> geometria) {
            this.geometria = geometria == null ? List.of() : geometria;
            return this;
        }

        public SegmentoVia construir() {
            return new SegmentoVia(this);
        }
    }
}
