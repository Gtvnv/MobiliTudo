package br.com.mobilitudo.obstaculos.infraestrutura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Hash do token de exclusão de um relato, em tabela própria — ver a migração V3 e
 * {@code RepositorioTokensDeExclusao} para o porquê da separação.
 */
@Entity
@Table(name = "obstaculo_token_exclusao")
public class ObstaculoTokenExclusaoEntidade {

    @Id
    @Column(name = "obstaculo_id")
    private UUID obstaculoId;

    @Column(name = "hash_token", nullable = false, length = 64)
    private String hashToken;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected ObstaculoTokenExclusaoEntidade() {
        // exigido pelo JPA
    }

    ObstaculoTokenExclusaoEntidade(UUID obstaculoId, String hashToken) {
        this.obstaculoId = obstaculoId;
        this.hashToken = hashToken;
        this.criadoEm = Instant.now();
    }

    String hashToken() {
        return hashToken;
    }
}
