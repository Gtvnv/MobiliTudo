package br.com.mobilitudo.compartilhado.dominio;

/**
 * Retangulo geografico delimitado pelos cantos sudoeste e nordeste.
 *
 * <p>E o recorte que as consultas espaciais usam para carregar apenas a vizinhanca
 * relevante da malha, em vez da cidade inteira.</p>
 */
public record AreaRetangular(Coordenada sudoeste, Coordenada nordeste) {

    /** Metros por grau de latitude; constante o bastante na escala de um municipio. */
    private static final double METROS_POR_GRAU_LATITUDE = 111_320.0;

    public AreaRetangular {
        if (sudoeste.latitude() > nordeste.latitude() || sudoeste.longitude() > nordeste.longitude()) {
            throw new IllegalArgumentException("Canto sudoeste deve ficar abaixo e a esquerda do nordeste");
        }
    }

    /**
     * Menor retangulo que contem os dois pontos, dilatado pela margem informada.
     *
     * <p>A margem e o que permite ao roteador considerar desvios: o caminho acessivel
     * costuma sair da reta entre origem e destino, e sem folga ele ficaria de fora.</p>
     */
    public static AreaRetangular envolvendo(Coordenada primeira, Coordenada segunda, double margemMetros) {
        double latitudeMinima = Math.min(primeira.latitude(), segunda.latitude());
        double latitudeMaxima = Math.max(primeira.latitude(), segunda.latitude());
        double longitudeMinima = Math.min(primeira.longitude(), segunda.longitude());
        double longitudeMaxima = Math.max(primeira.longitude(), segunda.longitude());

        double margemLatitude = margemMetros / METROS_POR_GRAU_LATITUDE;
        double latitudeMedia = Math.toRadians((latitudeMinima + latitudeMaxima) / 2);
        // Os meridianos se aproximam conforme se afasta do equador, entao a margem em graus
        // de longitude cresce com a latitude.
        double margemLongitude = margemMetros / (METROS_POR_GRAU_LATITUDE * Math.max(0.01, Math.cos(latitudeMedia)));

        return new AreaRetangular(
                new Coordenada(
                        Math.max(-90, latitudeMinima - margemLatitude),
                        Math.max(-180, longitudeMinima - margemLongitude)),
                new Coordenada(
                        Math.min(90, latitudeMaxima + margemLatitude),
                        Math.min(180, longitudeMaxima + margemLongitude)));
    }

    public boolean contem(Coordenada coordenada) {
        return coordenada.latitude() >= sudoeste.latitude()
                && coordenada.latitude() <= nordeste.latitude()
                && coordenada.longitude() >= sudoeste.longitude()
                && coordenada.longitude() <= nordeste.longitude();
    }
}
