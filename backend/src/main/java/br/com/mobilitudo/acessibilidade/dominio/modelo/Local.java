package br.com.mobilitudo.acessibilidade.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.NivelAcessibilidade;
import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Ponto de interesse mapeado, com os recursos de acessibilidade levantados em campo.
 *
 * <p>Nao guarda um "nivel de acessibilidade" fixo de proposito. Acessibilidade so faz
 * sentido em relacao a alguem: um museu com elevador e piso tatil e pleno para quem usa
 * cadeira de rodas e pode ser apenas razoavel para quem nao enxerga. Por isso o nivel e
 * calculado sob demanda em {@link #nivelPara(PerfilMobilidade)}.</p>
 */
public final class Local {

    private final UUID id;
    private final String nome;
    private final CategoriaLocal categoria;
    private final Coordenada coordenada;
    private final String endereco;
    private final String descricao;
    private final String imagemUrl;
    private final Set<RecursoAcessibilidade> recursos;
    private final Instant atualizadoEm;

    public Local(UUID id,
                 String nome,
                 CategoriaLocal categoria,
                 Coordenada coordenada,
                 String endereco,
                 String descricao,
                 String imagemUrl,
                 Set<RecursoAcessibilidade> recursos,
                 Instant atualizadoEm) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.nome = exigirTexto(nome, "nome do local");
        this.categoria = categoria == null ? CategoriaLocal.OUTRO : categoria;
        this.coordenada = Objects.requireNonNull(coordenada, "coordenada do local e obrigatoria");
        this.endereco = endereco;
        this.descricao = descricao;
        this.imagemUrl = imagemUrl;
        this.recursos = recursos == null || recursos.isEmpty()
                ? EnumSet.noneOf(RecursoAcessibilidade.class)
                : EnumSet.copyOf(recursos);
        this.atualizadoEm = atualizadoEm == null ? Instant.now() : atualizadoEm;
    }

    public boolean possui(RecursoAcessibilidade recurso) {
        return recursos.contains(recurso);
    }

    /**
     * Quao bem este local atende a um perfil especifico.
     *
     * <p>A regra de cada perfil espelha o que a falta do recurso significa na pratica:
     * sem acesso vertical, uma cadeira de rodas simplesmente nao entra; sem piso tatil
     * nem braille, quem nao enxerga entra mas nao se localiza.</p>
     */
    public NivelAcessibilidade nivelPara(PerfilMobilidade perfil) {
        return switch (perfil) {
            case CADEIRA_DE_RODAS -> {
                boolean acessoVertical = possui(RecursoAcessibilidade.RAMPA)
                        || possui(RecursoAcessibilidade.ELEVADOR);
                if (!acessoVertical) {
                    yield NivelAcessibilidade.INACESSIVEL;
                }
                yield possui(RecursoAcessibilidade.BANHEIRO_ADAPTADO)
                        ? NivelAcessibilidade.PLENO
                        : NivelAcessibilidade.COM_RESSALVAS;
            }
            case MOBILIDADE_REDUZIDA -> {
                boolean acessoVertical = possui(RecursoAcessibilidade.RAMPA)
                        || possui(RecursoAcessibilidade.ELEVADOR);
                if (!acessoVertical) {
                    yield NivelAcessibilidade.COM_RESSALVAS;
                }
                yield possui(RecursoAcessibilidade.CORRIMAO) || possui(RecursoAcessibilidade.ATENDIMENTO_PRIORITARIO)
                        ? NivelAcessibilidade.PLENO
                        : NivelAcessibilidade.COM_RESSALVAS;
            }
            case DEFICIENCIA_VISUAL -> {
                if (!possui(RecursoAcessibilidade.PISO_TATIL)) {
                    yield NivelAcessibilidade.INACESSIVEL;
                }
                yield possui(RecursoAcessibilidade.PLACA_BRAILLE)
                        ? NivelAcessibilidade.PLENO
                        : NivelAcessibilidade.COM_RESSALVAS;
            }
            case PADRAO -> recursos.isEmpty()
                    ? NivelAcessibilidade.COM_RESSALVAS
                    : NivelAcessibilidade.PLENO;
        };
    }

    public double distanciaEmMetrosAte(Coordenada referencia) {
        return coordenada.distanciaEmMetrosAte(referencia);
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public CategoriaLocal categoria() {
        return categoria;
    }

    public Coordenada coordenada() {
        return coordenada;
    }

    public String endereco() {
        return endereco;
    }

    public String descricao() {
        return descricao;
    }

    public String imagemUrl() {
        return imagemUrl;
    }

    public Set<RecursoAcessibilidade> recursos() {
        return Set.copyOf(recursos);
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Local local && id.equals(local.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio");
        }
        return valor.trim();
    }
}
