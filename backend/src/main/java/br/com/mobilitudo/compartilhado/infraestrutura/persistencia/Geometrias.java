package br.com.mobilitudo.compartilhado.infraestrutura.persistencia;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;

/**
 * Ponte entre o objeto de valor {@link Coordenada} do dominio e as geometrias JTS
 * que o Hibernate Spatial grava nas colunas {@code geometry(...,4326)} do PostGIS.
 *
 * <p>Fica na infraestrutura justamente para que o dominio nao precise conhecer JTS.
 * Atencao a ordem: no PostGIS o eixo X e a longitude e o Y e a latitude, invertida em
 * relacao a como se fala e se escreve uma coordenada.</p>
 */
public final class Geometrias {

    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory FABRICA =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private Geometrias() {
    }

    public static Point ponto(Coordenada coordenada) {
        return FABRICA.createPoint(new Coordinate(coordenada.longitude(), coordenada.latitude()));
    }

    public static Coordenada coordenada(Point ponto) {
        return new Coordenada(ponto.getY(), ponto.getX());
    }

    public static LineString linha(List<Coordenada> coordenadas) {
        Coordinate[] pontos = coordenadas.stream()
                .map(coordenada -> new Coordinate(coordenada.longitude(), coordenada.latitude()))
                .toArray(Coordinate[]::new);
        return FABRICA.createLineString(pontos);
    }

    public static List<Coordenada> coordenadas(LineString linha) {
        if (linha == null) {
            return List.of();
        }
        return java.util.Arrays.stream(linha.getCoordinates())
                .map(ponto -> new Coordenada(ponto.y, ponto.x))
                .toList();
    }
}
