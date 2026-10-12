package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface ObstaculoJpaRepository extends JpaRepository<ObstaculoEntidade, UUID> {

    List<ObstaculoEntidade> findAllByStatusOrderByReportadoEmDesc(StatusValidacao status);

    @Query(value = """
            SELECT o.* FROM obstaculo o
            WHERE ST_DWithin(o.localizacao::geography,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :raioMetros)
            ORDER BY o.reportado_em DESC
            """, nativeQuery = true)
    List<ObstaculoEntidade> buscarNaArea(@Param("latitude") double latitude,
                                         @Param("longitude") double longitude,
                                         @Param("raioMetros") double raioMetros);
}
