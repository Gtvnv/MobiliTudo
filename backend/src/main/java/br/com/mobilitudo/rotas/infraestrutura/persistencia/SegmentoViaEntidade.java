package br.com.mobilitudo.rotas.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.infraestrutura.persistencia.Geometrias;
import br.com.mobilitudo.rotas.dominio.modelo.ObstaculoNoTrecho;
import br.com.mobilitudo.rotas.dominio.modelo.SegmentoVia;
import br.com.mobilitudo.rotas.dominio.modelo.TipoSuperficie;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.LineString;

import java.util.List;

/** Aresta da malha viaria, com os atributos de acessibilidade levantados em campo. */
@Entity
@Table(name = "segmento_via")
public class SegmentoViaEntidade {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "nome_via", length = 160)
    private String nomeVia;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "no_origem_id", nullable = false)
    private NoViaEntidade origem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "no_destino_id", nullable = false)
    private NoViaEntidade destino;

    @Column(name = "comprimento_metros")
    private Double comprimentoMetros;

    @Column(name = "inclinacao_percentual", nullable = false)
    private double inclinacaoPercentual;

    @Column(name = "largura_metros", nullable = false)
    private double larguraMetros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoSuperficie superficie;

    @Column(name = "possui_piso_tatil", nullable = false)
    private boolean possuiPisoTatil;

    @Column(name = "possui_rebaixamento_guia", nullable = false)
    private boolean possuiRebaixamentoDeGuia;

    @Column(nullable = false)
    private boolean travessia;

    @Column(name = "iluminacao_adequada", nullable = false)
    private boolean iluminacaoAdequada;

    @Column(nullable = false)
    private boolean bidirecional;

    @Column(name = "tracado", nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString tracado;

    protected SegmentoViaEntidade() {
        // exigido pelo JPA
    }

    public String id() {
        return id;
    }

    public boolean bidirecional() {
        return bidirecional;
    }

    /**
     * Converte para o modelo de dominio, anexando os obstaculos que valem para este trecho.
     *
     * <p>Os obstaculos chegam de fora porque pertencem a outro contexto: a tabela de
     * segmentos nao sabe nada sobre quem reportou o que.</p>
     */
    public SegmentoVia paraDominio(List<ObstaculoNoTrecho> obstaculos) {
        return SegmentoVia.comId(id)
                .nomeVia(nomeVia)
                .entre(origem.paraDominio(), destino.paraDominio())
                .comprimentoMetros(comprimentoMetros == null ? 0 : comprimentoMetros)
                .inclinacaoPercentual(inclinacaoPercentual)
                .larguraMetros(larguraMetros)
                .superficie(superficie)
                .possuiPisoTatil(possuiPisoTatil)
                .possuiRebaixamentoDeGuia(possuiRebaixamentoDeGuia)
                .travessia(travessia)
                .iluminacaoAdequada(iluminacaoAdequada)
                .obstaculos(obstaculos)
                .geometria(Geometrias.coordenadas(tracado))
                .construir();
    }
}
