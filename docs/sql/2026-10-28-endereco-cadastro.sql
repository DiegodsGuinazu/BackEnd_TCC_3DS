-- PostgreSQL. Execute antes de publicar a API se ddl-auto não for update.
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS cep varchar(255);
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS bairro varchar(255);
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS logradouro varchar(255);
ALTER TABLE user_prof ADD COLUMN IF NOT EXISTS numero varchar(255);
ALTER TABLE user_resp ADD COLUMN IF NOT EXISTS cep varchar(255);
ALTER TABLE user_resp ADD COLUMN IF NOT EXISTS bairro varchar(255);
ALTER TABLE user_resp ADD COLUMN IF NOT EXISTS logradouro varchar(255);
ALTER TABLE user_resp ADD COLUMN IF NOT EXISTS numero varchar(255);
