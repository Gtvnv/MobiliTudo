package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ObstaculoTokenExclusaoJpaRepository extends JpaRepository<ObstaculoTokenExclusaoEntidade, UUID> {
}
