package br.com.mobilitudo.curadores.infraestrutura.persistencia;

import br.com.mobilitudo.curadores.dominio.modelo.SessaoDeCurador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao_curador")
public class SessaoCuradorEntidade {

    @Id
    private UUID id;

    @Column(name = "hash_token", nullable = false, unique = true, length = 64)
    private String hashToken;

    @Column(name = "curador_id", nullable = false)
    private UUID curadorId;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    protected SessaoCuradorEntidade() {
        // exigido pelo JPA
    }

    public static SessaoCuradorEntidade de(SessaoDeCurador sessao, String hashToken) {
        SessaoCuradorEntidade entidade = new SessaoCuradorEntidade();
        entidade.id = sessao.id();
        entidade.hashToken = hashToken;
        entidade.curadorId = sessao.curadorId();
        entidade.criadaEm = sessao.criadaEm();
        entidade.expiraEm = sessao.expiraEm();
        return entidade;
    }

    public SessaoDeCurador paraDominio() {
        return new SessaoDeCurador(id, curadorId, criadaEm, expiraEm);
    }
}
