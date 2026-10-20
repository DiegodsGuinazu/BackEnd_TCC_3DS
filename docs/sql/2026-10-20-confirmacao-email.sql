BEGIN;
-- Tabela separada: nenhuma conta existente é modificada ou bloqueada.
CREATE TABLE IF NOT EXISTS confirmacao_email (
    email_hash VARCHAR(64) PRIMARY KEY,
    codigo_hash VARCHAR(64) NOT NULL,
    enviado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL,
    utilizado BOOLEAN NOT NULL
);
COMMIT;
