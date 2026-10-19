-- PostgreSQL. Migração aditiva, idempotente; faça backup antes de aplicar em produção.
BEGIN;
ALTER TABLE IF EXISTS user_prof ADD COLUMN IF NOT EXISTS versao_credenciais BIGINT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS user_resp ADD COLUMN IF NOT EXISTS versao_credenciais BIGINT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS user_admin ADD COLUMN IF NOT EXISTS versao_credenciais BIGINT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS recuperacao_senha (
    email_hash VARCHAR(64) PRIMARY KEY,
    codigo_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    enviado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL,
    versao_credenciais BIGINT NOT NULL,
    utilizado BOOLEAN NOT NULL
);
COMMIT;
-- Não remover versao_credenciais para desfazer: isso pode revalidar JWTs revogados.
-- Em rollback de emergência, desabilite a recuperação e rotacione JWT_SECRET.
