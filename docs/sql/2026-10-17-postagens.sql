-- PostgreSQL: executar antes do backend; reaplicação segura. Sem apagar postagens antigas.
BEGIN;
ALTER TABLE postagem ADD COLUMN IF NOT EXISTS resumo TEXT;
ALTER TABLE postagem ADD COLUMN IF NOT EXISTS referencias TEXT;
ALTER TABLE postagem ADD COLUMN IF NOT EXISTS imagem_data TEXT;
COMMIT;
