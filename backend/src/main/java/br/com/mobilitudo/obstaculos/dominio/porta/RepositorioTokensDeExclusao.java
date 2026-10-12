package br.com.mobilitudo.obstaculos.dominio.porta;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para o hash do token de exclusão de cada relato.
 *
 * <p>Separada de {@link RepositorioObstaculos} de propósito: a verificação do token
 * ({@code TokenDeExclusao.confere}) é regra de domínio pura, mas onde e como o hash é
 * guardado é decisão de infraestrutura — e mantê-las em portas distintas também evita
 * que salvar uma transição de status no obstáculo arrisque tocar, e sobrescrever, o
 * hash por engano.</p>
 */
public interface RepositorioTokensDeExclusao {

    void salvar(UUID obstaculoId, String hashToken);

    Optional<String> hashDe(UUID obstaculoId);

    /** Torna o token de uso único: chamado depois de uma exclusão bem-sucedida. */
    void remover(UUID obstaculoId);
}
