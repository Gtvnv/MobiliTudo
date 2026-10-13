-- Substitui o segredo compartilhado da curadoria (MOBILITUDO_CURADOR_TOKEN) por contas
-- individuais. O ganho não é só segurança (revogar uma conta sem precisar trocar um
-- segredo que todo mundo usava) — é atribuição: dá para saber quem validou o quê.

CREATE TABLE curador (
    id             UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    nome_exibicao  VARCHAR(160) NOT NULL,
    hash_senha     VARCHAR(255) NOT NULL, -- PBKDF2-HMAC-SHA256, formato próprio — ver HashDeSenha
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sessão de login: token opaco com expiração, revogável a qualquer momento (logout,
-- desativação da conta) — ao contrário de um JWT autocontido, que continua válido até
-- expirar mesmo que a conta seja desativada no meio do caminho. Só o hash do token é
-- guardado, mesmo raciocínio de obstaculo_token_exclusao (migração V3).
CREATE TABLE sessao_curador (
    id          UUID PRIMARY KEY,
    hash_token  VARCHAR(64) NOT NULL UNIQUE,
    curador_id  UUID NOT NULL REFERENCES curador (id) ON DELETE CASCADE,
    criada_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sessao_curador_hash_token ON sessao_curador (hash_token);
CREATE INDEX idx_sessao_curador_curador_id ON sessao_curador (curador_id);

-- Quem avaliou cada obstáculo — o que uma conta individual permite e um segredo
-- compartilhado não permitia. Nulo para obstáculos ainda pendentes, ou avaliados antes
-- desta migração (dado histórico que não existia quando o segredo compartilhado era a
-- única forma de curadoria).
--
-- E-mail guardado direto (sem FK para curador.id) de propósito: isto é o retrato de
-- quem avaliou NO MOMENTO da avaliação, não uma referência viva que deveria mudar se a
-- conta for editada ou removida depois — e evita acoplar o esquema de um contexto ao
-- outro, e uma junção a mais só para mostrar quem validou cada obstáculo numa listagem.
ALTER TABLE obstaculo ADD COLUMN avaliado_por_email VARCHAR(255);
