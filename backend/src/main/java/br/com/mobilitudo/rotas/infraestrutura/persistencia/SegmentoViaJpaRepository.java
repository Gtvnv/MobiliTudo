package br.com.mobilitudo.rotas.infraestrutura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface SegmentoViaJpaRepository extends JpaRepository<SegmentoViaEntidade, String> {

    /**
     * Todos os trechos que cruzam o retangulo informado.
     *
     * <p>O operador {@code &&} testa a intersecao de bounding boxes e e atendido pelo indice
     * GiST, o que mantem a consulta barata mesmo quando a malha cresce. O filtro e por
     * intersecao, e nao por conteudo, para nao perder trechos que apenas atravessam a area.</p>
     */
    @Query(value = """
            SELECT s.* FROM segmento_via s
            WHERE s.tracado && ST_MakeEnvelope(:oeste, :sul, :leste, :norte, 4326)
            """, nativeQuery = true)
    List<SegmentoViaEntidade> buscarNaArea(@Param("sul") double sul,
                                           @Param("oeste") double oeste,
                                           @Param("norte") double norte,
                                           @Param("leste") double leste);
}
