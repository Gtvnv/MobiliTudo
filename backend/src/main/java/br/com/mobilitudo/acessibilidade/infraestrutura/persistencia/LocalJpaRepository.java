package br.com.mobilitudo.acessibilidade.infraestrutura.persistencia;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface LocalJpaRepository extends JpaRepository<LocalEntidade, UUID> {

    List<LocalEntidade> findAllByCategoriaOrderByNomeAsc(CategoriaLocal categoria);

    List<LocalEntidade> findAllByOrderByNomeAsc();

    /**
     * Busca por proximidade delegada ao PostGIS.
     *
     * <p>{@code ST_DWithin} sobre {@code geography} mede em metros reais e usa o indice GiST,
     * entao o filtro acontece no banco em vez de carregar a tabela inteira para a aplicacao.</p>
     */
    @Query(value = """
            SELECT l.* FROM local l
            WHERE ST_DWithin(l.localizacao::geography,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :raioMetros)
            ORDER BY l.localizacao::geography <-> ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
            """, nativeQuery = true)
    List<LocalEntidade> buscarProximos(@Param("latitude") double latitude,
                                       @Param("longitude") double longitude,
                                       @Param("raioMetros") double raioMetros);
}
