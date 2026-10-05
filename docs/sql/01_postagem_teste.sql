-- Execute no PostgreSQL/Neon pelo DBeaver ou SQL Editor, depois do deploy da API.
-- A postagem usa os endpoints existentes: nenhuma alteração de contrato para o app.
BEGIN;
INSERT INTO postagem (titulo, conteudo_html, data_criacao, data_atualizacao, publicado_aprendizagem)
SELECT '[TESTE] Conheça a área de Aprendizagem',
       '<h2>Bem-vindo à Aprendizagem</h2><p>Esta é uma postagem de demonstração da EspectroCare para testar o fluxo de acesso.</p><h3>Como funciona</h3><ul><li>Visitantes podem consultar os cards da área de Aprendizagem.</li><li>Ao selecionar Saiba mais, é necessário entrar em uma conta.</li><li>Após o login, o artigo escolhido é aberto automaticamente.</li></ul><p>Conteúdo de teste: remova esta postagem ao concluir a validação.</p>',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM postagem WHERE titulo = '[TESTE] Conheça a área de Aprendizagem'
);
COMMIT;

-- Para remover SOMENTE a demonstração, execute separadamente:
-- DELETE FROM postagem WHERE titulo = '[TESTE] Conheça a área de Aprendizagem';
