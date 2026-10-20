# Diagnóstico de envio de recuperação

Após publicar esta atualização no backend, abra os Logs do serviço no Render.
Na inicialização, confira `Recuperação de senha: provedor=gmail, configurado=true.`.
`configurado=true` indica apenas campos preenchidos e formato do remetente válido;
não confirma que o Google aceitou as credenciais.

Solicite uma vez o código para uma conta cadastrada com e-mail real. Aguarde pelo
menos 60 segundos entre solicitações. O limite é de três solicitações por e-mail
em 15 minutos; se atingido, aguarde esse período.

Em caso de falha Gmail, procure `Falha Gmail no envio`. O campo `finalidade` distingue
`recuperacao_senha` de `confirmacao_cadastro`:

| Campo ou motivo | Interpretação e próximo passo |
| --- | --- |
| `etapa=autorizacao` | Falha ao obter um access token com as credenciais OAuth. |
| `etapa=envio` | Falha na chamada à API Gmail após obter um access token. |
| `invalid_grant` | A autorização de renovação não foi aceita. Autorize novamente com o script e confira se as três credenciais pertencem ao mesmo cliente OAuth. |
| `invalid_client` | Confira ID e segredo do cliente OAuth configurados no Render. |
| `SERVICE_DISABLED`, `accessNotConfigured` ou `serviceDisabled` | Confira se a API Gmail está habilitada no projeto do cliente OAuth usado. |
| `insufficientPermissions` ou `ACCESS_TOKEN_SCOPE_INSUFFICIENT` | Autorize novamente concedendo o escopo `gmail.send` solicitado pelo script. |
| `falha_conexao`, `http=0` | Não houve resposta HTTP utilizável; confira conectividade e disponibilidade do provedor. |
| `nao_identificado` | O motivo não está na lista de códigos públicos reconhecidos; informe apenas etapa e status HTTP para investigação. |

`Solicitação de envio de e-mail aceita pelo provedor.` registra
o retorno bem-sucedido da chamada. Não garante entrega na caixa de entrada;
confira também Enviados no remetente e spam no destinatário.

Os registros omitem credenciais, tokens, código de recuperação, destinatário,
corpo da resposta, mensagens livres do provedor e pilhas de exceção.
Compartilhe apenas as linhas de diagnóstico; não compartilhe variáveis secretas.

Os registros de diagnóstico não exigem troca de provedor nem de credenciais.
A confirmação para novos cadastros tem requisitos próprios de banco e clientes;
consulte [CONFIRMACAO_EMAIL_CADASTRO.md](CONFIRMACAO_EMAIL_CADASTRO.md).

Referências oficiais:
- https://developers.google.com/workspace/gmail/api/guides/handle-errors
- https://developers.google.com/identity/protocols/oauth2#expiration
