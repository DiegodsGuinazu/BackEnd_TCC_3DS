BEGIN;
-- Executar no PostgreSQL ANTES de publicar se o Render usar ddl-auto=validate.
-- Contas antigas continuam sem selo; nenhum registro é aprovado por migração.
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_status VARCHAR(24);
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_nome_conferido VARCHAR(255);
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_registro_conferido VARCHAR(255);
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_solicitado_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_conferido_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_revisor_id BIGINT;
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS crm_revisao BIGINT NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_user_prof_crm_pedidos ON user_prof(crm_status, crm_solicitado_em);
COMMIT;
-- Para desfazer o aplicativo, reverta o PR e mantenha estas colunas (não destrutivas).
-- Não é necessário apagar os dados das conferências.
