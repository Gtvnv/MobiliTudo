package br.com.mobilitudo.curadores.infraestrutura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CuradorJpaRepository extends JpaRepository<CuradorEntidade, UUID> {

    Optional<CuradorEntidade> findByEmail(String email);

    List<CuradorEntidade> findAllByOrderByCriadoEmAsc();
}
