-- EspectroCare - migração manual para permitir CPF opcional de responsável.
-- Executar somente no PostgreSQL/Neon após confirmar o schema de produção.
-- A operação é segura para repetição: DROP NOT NULL não remove UNIQUE nem dados existentes.

ALTER TABLE user_resp
    ALTER COLUMN cpf DROP NOT NULL;
