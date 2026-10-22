package br.com.mobilitudo.curadores.infraestrutura.persistencia;

import br.com.mobilitudo.curadores.dominio.modelo.Curador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "curador")
public class CuradorEntidade {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "nome_exibicao", nullable = false, length = 160)
    private String nomeExibicao;

    @Column(name = "hash_senha", nullable = false, length = 255)
    private String hashSenha;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected CuradorEntidade() {
        // exigido pelo JPA
    }

    public static CuradorEntidade de(Curador curador) {
        CuradorEntidade entidade = new CuradorEntidade();
        entidade.id = curador.id();
        entidade.email = curador.email();
        entidade.nomeExibicao = curador.nomeDeExibicao();
        entidade.hashSenha = curador.hashSenha();
        entidade.ativo = curador.ativo();
        entidade.criadoEm = curador.criadoEm();
        return entidade;
    }

    public Curador paraDominio() {
        return Curador.reconstituir(id, email, nomeExibicao, hashSenha, ativo, criadoEm);
    }
}
