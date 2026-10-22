package br.com.mobilitudo.compartilhado.infraestrutura.web;

import br.com.mobilitudo.compartilhado.dominio.PerfilMobilidade;

import java.util.Locale;

/**
 * Leitura tolerante do parametro {@code perfil} vindo da web.
 *
 * <p>Aceita {@code cadeira-de-rodas}, {@code CADEIRA_DE_RODAS} ou nada, e cai em
 * {@link PerfilMobilidade#PADRAO} quando nao reconhece. Um perfil escrito errado degrada
 * a recomendacao, nao derruba a consulta.</p>
 */
public final class Perfis {

    private Perfis() {
    }

    public static PerfilMobilidade interpretar(String valor) {
        if (valor == null || valor.isBlank()) {
            return PerfilMobilidade.PADRAO;
        }
        String normalizado = valor.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return PerfilMobilidade.valueOf(normalizado);
        } catch (IllegalArgumentException naoReconhecido) {
            return PerfilMobilidade.PADRAO;
        }
    }
}
