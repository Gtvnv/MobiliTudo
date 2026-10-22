package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import br.com.mobilitudo.obstaculos.dominio.modelo.StatusValidacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ObstaculoJpaRepository extends JpaRepository<ObstaculoEntidade, UUID> {

    List<ObstaculoEntidade> findAllByStatusOrderByReportadoEmDesc(StatusValidacao status, Pageable pagina);

    /**
     * {@code FOR UPDATE} nativo em vez de {@code @Lock}: mais direto que acoplar o modo
     * de trava à interação (às vezes inconsistente) entre {@code @Lock} e consulta
     * nativa entre provedores JPA — ver {@code RepositorioObstaculos.porIdParaAtualizar}
     * para o porquê desta trava existir.
     */
    @Query(value = "SELECT * FROM obstaculo WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<ObstaculoEntidade> buscarParaAtualizar(@Param("id") UUID id);

    // Sem Pageable: usada apenas pelo adaptador que alimenta o cálculo de rota, que
    // precisa de todo obstáculo validado, não de uma página — ver RepositorioObstaculos.
    List<ObstaculoEntidade> findAllByStatus(StatusValidacao status);

    @Query(value = """
            SELECT o.* FROM obstaculo o
            WHERE ST_DWithin(o.localizacao::geography,
                             ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                             :raioMetros)
            ORDER BY o.reportado_em DESC
            """, nativeQuery = true)
    List<ObstaculoEntidade> buscarNaArea(@Param("latitude") double latitude,
                                         @Param("longitude") double longitude,
                                         @Param("raioMetros") double raioMetros,
                                         Pageable pagina);
}
