package br.com.mobilitudo.compartilhado.dominio;

/**
 * Par latitude/longitude em WGS 84 (EPSG:4326).
 *
 * <p>Objeto de valor do nucleo do dominio: nao depende de JTS nem de qualquer
 * biblioteca de persistencia. A conversao para {@code geometry(Point,4326)} do
 * PostGIS acontece apenas na camada de infraestrutura.</p>
 */
public record Coordenada(double latitude, double longitude) {

    private static final double RAIO_TERRA_METROS = 6_371_008.8;

    public Coordenada {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude fora do intervalo [-90, 90]: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude fora do intervalo [-180, 180]: " + longitude);
        }
    }

    /**
     * Distancia de grande circulo (formula de haversine) em metros.
     *
     * <p>E usada como heuristica do A* no calculo de rotas, por isso precisa ser
     * sempre menor ou igual a distancia real percorrivel pela malha viaria.</p>
     */
    public double distanciaEmMetrosAte(Coordenada outra) {
        double deltaLat = Math.toRadians(outra.latitude - this.latitude);
        double deltaLon = Math.toRadians(outra.longitude - this.longitude);
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(outra.latitude);

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * RAIO_TERRA_METROS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Azimute em graus (0 = norte, 90 = leste) desta coordenada ate {@code outra}. */
    public double azimuteAte(Coordenada outra) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(outra.latitude);
        double deltaLon = Math.toRadians(outra.longitude - this.longitude);

        double y = Math.sin(deltaLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
