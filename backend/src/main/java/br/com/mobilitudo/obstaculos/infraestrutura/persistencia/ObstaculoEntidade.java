package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;
import br.com.mobilitudo.compartilhado.infraestrutura.persistencia.Geometrias;
import br.com.mobilitudo.obstaculos.dominio.modelo.Obstaculo;
import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

/** Mapeamento relacional do agregado {@link Obstaculo}. */
@Entity
@Table(name = "obstaculo")
public class ObstaculoEntidade {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoObstaculo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severidade severidade;

    @Column(name = "localizacao", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point localizacao;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "segmento_via_id", length = 64)
    private String segmentoViaId;

    @Column(name = "reportado_por", nullable = false, length = 120)
    private String reportadoPor;

    @Column(name = "reportado_em", nullable = false)
    private Instant reportadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatusValidacao status;

    @Column(name = "avaliado_em")
    private Instant avaliadoEm;

    @Column(name = "avaliado_por_email", length = 255)
    private String avaliadoPorEmail;

    protected ObstaculoEntidade() {
        // exigido pelo JPA
    }

    public static ObstaculoEntidade de(Obstaculo obstaculo) {
        ObstaculoEntidade entidade = new ObstaculoEntidade();
        entidade.id = obstaculo.id();
        entidade.tipo = obstaculo.tipo();
        entidade.severidade = obstaculo.severidade();
        entidade.localizacao = Geometrias.ponto(obstaculo.coordenada());
        entidade.descricao = obstaculo.descricao();
        entidade.segmentoViaId = obstaculo.segmentoViaId().orElse(null);
        entidade.reportadoPor = obstaculo.reportadoPor();
        entidade.reportadoEm = obstaculo.reportadoEm();
        entidade.status = obstaculo.status();
        entidade.avaliadoEm = obstaculo.avaliadoEm().orElse(null);
        entidade.avaliadoPorEmail = obstaculo.avaliadoPorEmail().orElse(null);
        return entidade;
    }

    public Obstaculo paraDominio() {
        return Obstaculo.reconstituir(id, tipo, severidade, Geometrias.coordenada(localizacao),
                descricao, segmentoViaId, reportadoPor, reportadoEm, status, avaliadoEm, avaliadoPorEmail);
    }
}
