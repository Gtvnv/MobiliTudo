-- Suporte a autoexclusao de dado pessoal (LGPD art. 18): quem reporta um obstaculo
-- recebe, uma unica vez, um token de exclusao. Guardamos so o hash SHA-256 dele —
-- nunca o texto puro — para que um vazamento do banco nao equivalha a vazar os
-- proprios tokens. A tabela e separada de "obstaculo" de proposito: assim, salvar
-- uma transicao de status (validar/rejeitar/resolver) nunca corre o risco de
-- sobrescrever ou apagar o hash por engano, porque simplesmente nao toca nesta
-- tabela.

CREATE TABLE obstaculo_token_exclusao (
    obstaculo_id UUID PRIMARY KEY REFERENCES obstaculo (id) ON DELETE CASCADE,
    hash_token   VARCHAR(64) NOT NULL, -- SHA-256 em hexadecimal: sempre 64 caracteres
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sem indice extra: a unica consulta e por obstaculo_id, ja coberto pela chave primaria.
