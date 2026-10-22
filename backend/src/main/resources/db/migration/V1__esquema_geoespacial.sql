-- Esquema geoespacial do MobiliTudo.
--
-- Todas as geometrias usam SRID 4326 (WGS 84), o mesmo sistema das coordenadas que
-- chegam do GPS do navegador e do Leaflet, o que dispensa reprojecao no caminho.
-- As consultas por distancia convertem para ::geography na hora de medir, para que o
-- raio seja dado em metros reais e nao em graus.

CREATE EXTENSION IF NOT EXISTS postgis;

-- ---------------------------------------------------------------------------
-- Acervo de locais acessiveis
-- ---------------------------------------------------------------------------

CREATE TABLE local (
    id            UUID PRIMARY KEY,
    nome          VARCHAR(160) NOT NULL,
    categoria     VARCHAR(32)  NOT NULL,
    localizacao   geometry(Point, 4326) NOT NULL,
    endereco      VARCHAR(255),
    descricao     TEXT,
    imagem_url    VARCHAR(512),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_local_localizacao ON local USING GIST (localizacao);
CREATE INDEX idx_local_categoria   ON local (categoria);

CREATE TABLE local_recurso (
    local_id UUID        NOT NULL REFERENCES local (id) ON DELETE CASCADE,
    recurso  VARCHAR(40) NOT NULL,
    PRIMARY KEY (local_id, recurso)
);

-- ---------------------------------------------------------------------------
-- Malha viaria: o grafo sobre o qual as rotas sao calculadas
-- ---------------------------------------------------------------------------

CREATE TABLE no_via (
    id          VARCHAR(64) PRIMARY KEY,
    localizacao geometry(Point, 4326) NOT NULL
);

CREATE INDEX idx_no_via_localizacao ON no_via USING GIST (localizacao);

CREATE TABLE segmento_via (
    id                       VARCHAR(64) PRIMARY KEY,
    nome_via                 VARCHAR(160),
    no_origem_id             VARCHAR(64) NOT NULL REFERENCES no_via (id),
    no_destino_id            VARCHAR(64) NOT NULL REFERENCES no_via (id),
    comprimento_metros       DOUBLE PRECISION,
    -- Positiva quando o trecho sobe da origem para o destino.
    inclinacao_percentual    DOUBLE PRECISION NOT NULL DEFAULT 0,
    -- Faixa livre de circulacao; a NBR 9050 pede ao menos 1,20 m.
    largura_metros           DOUBLE PRECISION NOT NULL DEFAULT 1.50,
    superficie               VARCHAR(32)  NOT NULL DEFAULT 'DESCONHECIDA',
    possui_piso_tatil        BOOLEAN      NOT NULL DEFAULT FALSE,
    possui_rebaixamento_guia BOOLEAN      NOT NULL DEFAULT TRUE,
    travessia                BOOLEAN      NOT NULL DEFAULT FALSE,
    iluminacao_adequada      BOOLEAN      NOT NULL DEFAULT TRUE,
    bidirecional             BOOLEAN      NOT NULL DEFAULT TRUE,
    tracado                  geometry(LineString, 4326) NOT NULL,

    CONSTRAINT ck_segmento_nos_distintos CHECK (no_origem_id <> no_destino_id),
    CONSTRAINT ck_segmento_largura_positiva CHECK (largura_metros > 0)
);

CREATE INDEX idx_segmento_via_tracado ON segmento_via USING GIST (tracado);
CREATE INDEX idx_segmento_via_origem  ON segmento_via (no_origem_id);
CREATE INDEX idx_segmento_via_destino ON segmento_via (no_destino_id);

-- ---------------------------------------------------------------------------
-- Obstaculos reportados pela comunidade
-- ---------------------------------------------------------------------------

CREATE TABLE obstaculo (
    id              UUID PRIMARY KEY,
    tipo            VARCHAR(40) NOT NULL,
    severidade      VARCHAR(16) NOT NULL,
    localizacao     geometry(Point, 4326) NOT NULL,
    descricao       TEXT,
    segmento_via_id VARCHAR(64) REFERENCES segmento_via (id) ON DELETE SET NULL,
    reportado_por   VARCHAR(120) NOT NULL,
    reportado_em    TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    avaliado_em     TIMESTAMPTZ,

    CONSTRAINT ck_obstaculo_status
        CHECK (status IN ('PENDENTE', 'VALIDADO', 'REJEITADO', 'RESOLVIDO'))
);

CREATE INDEX idx_obstaculo_localizacao ON obstaculo USING GIST (localizacao);
CREATE INDEX idx_obstaculo_status      ON obstaculo (status);
CREATE INDEX idx_obstaculo_segmento    ON obstaculo (segmento_via_id);
