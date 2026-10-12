package br.com.mobilitudo.acessibilidade.infraestrutura.persistencia;

import br.com.mobilitudo.acessibilidade.dominio.modelo.CategoriaLocal;
import br.com.mobilitudo.acessibilidade.dominio.modelo.Local;
import br.com.mobilitudo.acessibilidade.dominio.modelo.RecursoAcessibilidade;
import br.com.mobilitudo.compartilhado.infraestrutura.persistencia.Geometrias;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Mapeamento relacional do agregado {@link Local}. */
@Entity
@Table(name = "local")
public class LocalEntidade {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CategoriaLocal categoria;

    @Column(name = "localizacao", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point localizacao;

    @Column(length = 255)
    private String endereco;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "imagem_url", length = 512)
    private String imagemUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "local_recurso", joinColumns = @JoinColumn(name = "local_id"))
    @Column(name = "recurso", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<RecursoAcessibilidade> recursos = new HashSet<>();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected LocalEntidade() {
        // exigido pelo JPA
    }

    public static LocalEntidade de(Local local) {
        LocalEntidade entidade = new LocalEntidade();
        entidade.id = local.id();
        entidade.nome = local.nome();
        entidade.categoria = local.categoria();
        entidade.localizacao = Geometrias.ponto(local.coordenada());
        entidade.endereco = local.endereco();
        entidade.descricao = local.descricao();
        entidade.imagemUrl = local.imagemUrl();
        entidade.recursos = new HashSet<>(local.recursos());
        entidade.atualizadoEm = local.atualizadoEm();
        return entidade;
    }

    public Local paraDominio() {
        return new Local(
                id,
                nome,
                categoria,
                Geometrias.coordenada(localizacao),
                endereco,
                descricao,
                imagemUrl,
                recursos.isEmpty() ? EnumSet.noneOf(RecursoAcessibilidade.class) : EnumSet.copyOf(recursos),
                atualizadoEm);
    }
}
