package br.com.mobilitudo.obstaculos.dominio.modelo;

import br.com.mobilitudo.compartilhado.dominio.Coordenada;
import br.com.mobilitudo.compartilhado.dominio.Severidade;
import br.com.mobilitudo.compartilhado.dominio.TipoObstaculo;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Barreira urbana reportada por um usuario.
 *
 * <p>E o agregado que sustenta a parte colaborativa do MobiliTudo: qualquer pessoa relata,
 * e o relato so passa a desviar rotas depois de validado. O estado muda apenas pelos metodos
 * de transicao, que recusam caminhos invalidos em vez de deixar o status virar um campo livre.</p>
 */
public final class Obstaculo {

    private final UUID id;
    private final TipoObstaculo tipo;
    private final Severidade severidade;
    private final Coordenada coordenada;
    private final String descricao;
    private final String segmentoViaId;
    private String reportadoPor;
    private final Instant reportadoEm;

    private StatusValidacao status;
    private Instant avaliadoEm;

    private Obstaculo(UUID id,
                      TipoObstaculo tipo,
                      Severidade severidade,
                      Coordenada coordenada,
                      String descricao,
                      String segmentoViaId,
                      String reportadoPor,
                      Instant reportadoEm,
                      StatusValidacao status,
                      Instant avaliadoEm) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.tipo = Objects.requireNonNull(tipo, "tipo do obstaculo e obrigatorio");
        this.severidade = Objects.requireNonNull(severidade, "severidade do obstaculo e obrigatoria");
        this.coordenada = Objects.requireNonNull(coordenada, "coordenada do obstaculo e obrigatoria");
        this.descricao = descricao;
        this.segmentoViaId = segmentoViaId;
        this.reportadoPor = reportadoPor == null || reportadoPor.isBlank() ? "anônimo" : reportadoPor.trim();
        this.reportadoEm = reportadoEm == null ? Instant.now() : reportadoEm;
        this.status = status == null ? StatusValidacao.PENDENTE : status;
        this.avaliadoEm = avaliadoEm;
    }

    /** Novo relato, sempre pendente de conferencia. */
    public static Obstaculo reportar(TipoObstaculo tipo,
                                     Severidade severidade,
                                     Coordenada coordenada,
                                     String descricao,
                                     String segmentoViaId,
                                     String reportadoPor) {
        return new Obstaculo(null, tipo, severidade, coordenada, descricao, segmentoViaId,
                reportadoPor, Instant.now(), StatusValidacao.PENDENTE, null);
    }

    /** Reidrata o agregado a partir do banco, sem passar pelas regras de transicao. */
    public static Obstaculo reconstituir(UUID id,
                                         TipoObstaculo tipo,
                                         Severidade severidade,
                                         Coordenada coordenada,
                                         String descricao,
                                         String segmentoViaId,
                                         String reportadoPor,
                                         Instant reportadoEm,
                                         StatusValidacao status,
                                         Instant avaliadoEm) {
        return new Obstaculo(id, tipo, severidade, coordenada, descricao, segmentoViaId,
                reportadoPor, reportadoEm, status, avaliadoEm);
    }

    public void validar() {
        exigirPendente(StatusValidacao.VALIDADO);
        this.status = StatusValidacao.VALIDADO;
        this.avaliadoEm = Instant.now();
    }

    public void rejeitar() {
        exigirPendente(StatusValidacao.REJEITADO);
        this.status = StatusValidacao.REJEITADO;
        this.avaliadoEm = Instant.now();
    }

    /**
     * Marca a barreira como resolvida depois de uma intervencao na via.
     *
     * <p>So faz sentido para um obstaculo que chegou a existir de fato, ou seja, que foi
     * validado antes. Resolver um relato rejeitado seria afirmar que a cidade consertou
     * algo que nunca esteve la.</p>
     */
    public void marcarResolvido() {
        if (status != StatusValidacao.VALIDADO) {
            throw new TransicaoInvalidaException(status, StatusValidacao.RESOLVIDO);
        }
        this.status = StatusValidacao.RESOLVIDO;
        this.avaliadoEm = Instant.now();
    }

    public boolean afetaRoteamento() {
        return status.afetaRoteamento();
    }

    /**
     * Remove o nome de quem relatou, independentemente do status da barreira.
     *
     * <p>Autoexclusão de dado pessoal (LGPD art. 18) é um direito sobre a identidade de
     * quem relatou, não sobre a informação de acessibilidade do local — por isso isto
     * apaga só o nome, nunca o relato inteiro: a rampa continua faltando mesmo depois
     * que ninguém mais sabe quem avisou disso. Idempotente: chamar de novo sobre um
     * relato já anônimo não muda nada.</p>
     */
    public void anonimizar() {
        this.reportadoPor = "anônimo";
    }

    private void exigirPendente(StatusValidacao pretendido) {
        if (status != StatusValidacao.PENDENTE) {
            throw new TransicaoInvalidaException(status, pretendido);
        }
    }

    public UUID id() {
        return id;
    }

    public TipoObstaculo tipo() {
        return tipo;
    }

    public Severidade severidade() {
        return severidade;
    }

    public Coordenada coordenada() {
        return coordenada;
    }

    public String descricao() {
        return descricao;
    }

    public Optional<String> segmentoViaId() {
        return Optional.ofNullable(segmentoViaId);
    }

    public String reportadoPor() {
        return reportadoPor;
    }

    public Instant reportadoEm() {
        return reportadoEm;
    }

    public StatusValidacao status() {
        return status;
    }

    public Optional<Instant> avaliadoEm() {
        return Optional.ofNullable(avaliadoEm);
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Obstaculo obstaculo && id.equals(obstaculo.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
