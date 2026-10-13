package br.com.mobilitudo.curadores.infraestrutura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SessaoCuradorJpaRepository extends JpaRepository<SessaoCuradorEntidade, UUID> {

    Optional<SessaoCuradorEntidade> findByHashToken(String hashToken);
}
