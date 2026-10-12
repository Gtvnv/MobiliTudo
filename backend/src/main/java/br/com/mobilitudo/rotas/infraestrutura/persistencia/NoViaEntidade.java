package br.com.mobilitudo.rotas.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.infraestrutura.persistencia.Geometrias;
import br.com.mobilitudo.rotas.dominio.modelo.NoVia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

/** Vertice da malha viaria persistido como {@code geometry(Point,4326)}. */
@Entity
@Table(name = "no_via")
public class NoViaEntidade {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "localizacao", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point localizacao;

    protected NoViaEntidade() {
        // exigido pelo JPA
    }

    public NoVia paraDominio() {
        return new NoVia(id, Geometrias.coordenada(localizacao));
    }
}
