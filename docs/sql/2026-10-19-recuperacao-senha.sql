-- PostgreSQL: executar antes do deploy. Não altera senhas/CPFs existentes.
BEGIN;
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_resp ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE user_resp ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS recuperacao_senha (
    email VARCHAR(254) PRIMARY KEY,
    codigo_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    solicitado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL,
    token_version INTEGER NOT NULL,
    tipo_perfil VARCHAR(255) NOT NULL,
    usuario_id INTEGER NOT NULL,
    usado BOOLEAN NOT NULL
);
COMMIT;
