# Confirmação de e-mail para novos cadastros

Aplica-se somente à criação de profissionais e responsáveis. Contas existentes,
login, JWTs, recuperação de senha e cadastro de administradores por convite
continuam com seus contratos. Nenhuma conta anterior é marcada como pendente.

## Fluxo e contrato

1. Validar os campos do formulário.
2. `POST /cadastro/email/solicitar` com `{"email":"pessoa@exemplo.com"}`.
3. Exibir o campo do código recebido, opção de reenviar após 60 segundos e opção
   de corrigir o endereço. A resposta é genérica, inclusive para contas existentes;
   nesses casos não há envio e o usuário deve fazer login.
4. `POST /cadastro/profissional` ou `/cadastro/responsavel` com os campos anteriores
   e o campo adicional `codigoEmail` (string com oito dígitos, preservando zeros).
5. Somente um código válido permite salvar a conta. Sucesso permanece HTTP 200 e
   texto; código ausente, incorreto, expirado ou usado retorna HTTP 422:

```json
{"codigo":"EMAIL_NAO_CONFIRMADO","mensagem":"E-mail não confirmado. Código inválido, expirado ou já utilizado. Confira o código ou solicite outro."}
```

Um envio aceito pela API não prova entrega. A confirmação do código demonstra
acesso à caixa de e-mail; a ausência dele não prova que um endereço seja falso.
Os modelos de e-mail de cadastro e recuperação são distintos, assim como seus
registros e hashes: um código de recuperação não confirma um cadastro.

## Segurança e dados

- Código criptograficamente aleatório, oito dígitos, 15 minutos, cinco tentativas.
- Reenvio com intervalo mínimo de 60 segundos; novo código invalida o anterior.
- Até três solicitações por e-mail e 20 por IP em 15 minutos, por instância,
  compartilhando o mecanismo de limites existente mas com chaves separadas.
- Código e endereço são armazenados como HMAC-SHA256 em tabela separada; os
  dados do cadastro e a senha ficam na memória do cliente até a confirmação.
- A conta e o consumo do código são salvos na mesma transação, com bloqueio do
  registro. Tentativas incorretas persistem mesmo com resposta HTTP 422.
- A primeira solicitação simultânea pode retornar 409; aguardar um minuto antes
  de reenviar. O evento de envio só é processado após o commit.
- Limpeza horária dos registros expirados há mais de um dia.
- Logs não incluem endereço, código ou credenciais; diagnóstico Gmail informa
  `finalidade=confirmacao_cadastro` ou `finalidade=recuperacao_senha`.
- O remetente continua sendo a conta já configurada em `GMAIL_SENDER`. Não é
  necessário outro projeto Google, chave, token ou configuração Resend.

## Publicação coordenada

1. Revisar e aplicar `docs/sql/2026-10-20-confirmacao-email.sql` no Neon. É
   idempotente, não atualiza tabelas de usuários e não apaga dados. Com
   `ddl-auto=update`, Hibernate também cria a tabela automaticamente.
2. Preparar a versão nova do site e distribuir a atualização do aplicativo.
3. Publicar o backend junto da ativação dos clientes atualizados.

Versões antigas dos clientes continuam fazendo login com contas existentes,
mas **não conseguem cadastrar novas contas** após a publicação do backend: o
campo `codigoEmail` passa a ser obrigatório para criar profissionais/responsáveis.
Não existe fallback sem confirmação. Durante a publicação, os clientes novos
precisam do novo endpoint disponível para enviar códigos.

## Testes manuais

- Conta anterior: entrar normalmente, recuperar senha e consultar perfil.
- Novo responsável e profissional: cadastrar, receber o e-mail com assunto
  “EspectroCare — confirme seu e-mail”, confirmar o código e fazer login.
- Código errado ou expirado: permanecer na confirmação com mensagem clara.
- Corrigir e-mail: voltar aos dados sem criar conta; solicitar para o novo endereço.
- Reenviar: respeitar 60 segundos, usar o código mais recente.
- Cinco erros: bloquear o código; reenviar respeitando intervalo e limites.
- Site: teclado, Escape, leitores de tela, celular e duplo clique.
- App: rotação durante envio/validação; sem salvar senha ou código em Bundle,
  Intent, disco ou logs. Se o processo for encerrado, reiniciar o cadastro.

Os testes automatizados usam o provedor simulado. A entrega real depende das
credenciais e da API Gmail habilitada no projeto Google Cloud correto.
