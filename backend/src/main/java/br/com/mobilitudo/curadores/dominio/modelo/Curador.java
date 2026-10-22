package br.com.mobilitudo.curadores.dominio.modelo;

import br.com.mobilitudo.curadores.dominio.servico.HashDeSenha;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma pessoa com acesso à curadoria de obstáculos: validar, rejeitar, resolver um
 * relato, e cadastrar ou desativar outras contas de curador.
 *
 * <p>Substitui o antigo segredo compartilhado (um único {@code MOBILITUDO_CURADOR_TOKEN}
 * que qualquer curador usava) por contas individuais. O ganho não é só poder revogar o
 * acesso de uma pessoa sem precisar trocar um segredo que todo mundo usava — é
 * atribuição: cada obstáculo avaliado passa a registrar quem avaliou.</p>
 *
 * <p>Não há hierarquia de papéis: qualquer curador ativo pode cadastrar ou desativar
 * outro. Para o tamanho atual do grupo de curadoria, uma distinção administrador/curador
 * comum adicionaria complexidade sem necessidade real ainda — pode ser introduzida
 * depois, se o grupo crescer a ponto de precisar.</p>
 */
public final class Curador {

    private final UUID id;
    private final String email;
    private final String nomeDeExibicao;
    private final String hashSenha;
    private final Instant criadoEm;

    private boolean ativo;

    private Curador(UUID id, String email, String nomeDeExibicao, String hashSenha,
                    boolean ativo, Instant criadoEm) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.email = Objects.requireNonNull(email, "email do curador é obrigatório").trim().toLowerCase();
        this.nomeDeExibicao = exigirTexto(nomeDeExibicao, "nome de exibição do curador");
        this.hashSenha = Objects.requireNonNull(hashSenha, "hash de senha é obrigatório");
        this.ativo = ativo;
        this.criadoEm = criadoEm == null ? Instant.now() : criadoEm;
    }

    /** Nova conta — a senha em texto puro só existe até virar hash, aqui dentro. */
    public static Curador criar(String email, String nomeDeExibicao, String senhaEmTextoPuro) {
        if (senhaEmTextoPuro == null || senhaEmTextoPuro.length() < 8) {
            throw new SenhaFracaException();
        }
        return new Curador(null, email, nomeDeExibicao, HashDeSenha.hash(senhaEmTextoPuro), true, Instant.now());
    }

    /** Reidrata o agregado a partir do banco, sem passar pela validação de senha nova. */
    public static Curador reconstituir(UUID id, String email, String nomeDeExibicao, String hashSenha,
                                       boolean ativo, Instant criadoEm) {
        return new Curador(id, email, nomeDeExibicao, hashSenha, ativo, criadoEm);
    }

    /**
     * Confere a senha E a conta estar ativa — as duas coisas, para que uma conta
     * desativada não consiga mais entrar mesmo sabendo a senha certa.
     */
    public boolean autenticaCom(String senhaEmTextoPuro) {
        return ativo && HashDeSenha.confere(senhaEmTextoPuro, hashSenha);
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String nomeDeExibicao() {
        return nomeDeExibicao;
    }

    public boolean ativo() {
        return ativo;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    /**
     * O hash, nunca a senha em texto puro — essa nunca chega a ser um campo deste
     * objeto. Exposto como qualquer outro dado do agregado, porque o hash é desenhado
     * para ser seguro mesmo que alguém o veja; só a camada de DTO da API, não o domínio,
     * é responsável por nunca deixá-lo chegar numa resposta HTTP.
     */
    public String hashSenha() {
        return hashSenha;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Curador curador && id.equals(curador.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor.trim();
    }
}
