# CPF e recuperação de senha — 19/10/2026

## Configuração e implantação

1. Aplique primeiro o PR do backend. O login e os cadastros mantêm os caminhos e campos existentes.
2. Faça um backup do PostgreSQL. Execute `docs/sql/2026-10-19-cpf-recuperacao-senha.sql` no banco do projeto (DBeaver ou console SQL do Neon). O script pode ser repetido. Com `ddl-auto=update`, Hibernate também cria a estrutura; o script permite revisar a mudança antes do deploy.
3. Crie uma conta na [Resend](https://resend.com), adicione um domínio que sua equipe controla e conclua a verificação DNS. Crie uma API key com permissão de envio para esse domínio. O domínio de teste da Resend limita destinatários; para atender todos os usuários, use um remetente de domínio verificado. Não use o endereço Gmail do usuário como remetente.
4. No serviço do **backend no Render → Environment**, cadastre:

   | Variável | Valor |
   | --- | --- |
   | `RESEND_API_KEY` | Chave privada gerada na Resend |
   | `MAIL_FROM` | `EspectroCare <nao-responda@SEU-DOMINIO>` |

   Mantenha `JWT_SECRET` e as variáveis do banco já configuradas. Nunca coloque essas chaves em Git, frontend, APK, ZIP ou screenshots. A integração usa HTTPS na porta 443, sem biblioteca nova e sem depender de SMTP.
5. Refaça o deploy do backend, depois publique o frontend e distribua o aplicativo atualizado. Nenhum e-mail real foi enviado durante os testes automatizados.
6. Teste com contas de responsável e profissional cujo e-mail sua equipe controla: solicite o código, confira entrada/spam, defina uma nova senha e entre novamente no site e app.

Se as variáveis estiverem ausentes, a API continua iniciando e o restante do sistema funciona; a recuperação retorna `503` para qualquer endereço. Com chave incorreta ou domínio não autorizado, a resposta pública continua genérica para não revelar contas. Consulte o painel da Resend e o aviso sanitizado nos logs do backend. Após corrigir, aguarde o intervalo e solicite outro código.

Documentação do provedor: [envio de e-mail](https://resend.com/docs/api-reference/emails/send-email) e [domínios](https://resend.com/docs/dashboard/domains/introduction).

## Contratos

Todas as novas rotas usam JSON e `Cache-Control: no-store`. POSTs públicos não exigem Bearer.

| Método/rota | Corpo | Resultado |
| --- | --- | --- |
| `POST /validacoes/cpf` | `{"cpf":"529.982.247-25"}` | `{"valido":true,"cadastroConsultado":false,"mensagem":"..."}` |
| `POST /auth/esqueci-senha` | `{"email":"conta@exemplo.com"}` | `200` e mensagem genérica, exista ou não uma conta |
| `POST /api/conta/recuperacao-senha` | Sem corpo; Bearer obrigatório | Envia para o e-mail da conta autenticada, obtido do banco |
| `POST /auth/redefinir-senha` | `{"email":"conta@exemplo.com","codigo":"12345678","novaSenha":"NovaSenha@123"}` | `200` e orientação para entrar novamente |

O CPF da tabela é apenas um vetor de teste do algoritmo. Não implica titularidade.

Erros da recuperação: `400` dados/código inválidos, expirados ou consumidos; `401` sessão inválida na solicitação autenticada; `429` limite excedido; `503` envio não configurado. O e-mail informado nunca muda o e-mail do cadastro. O endpoint autenticado ignora qualquer destinatário enviado no corpo.

## O que a validação de CPF faz

O backend rejeita CPF com dígitos incorretos, caracteres indevidos, comprimento incorreto e sequências repetidas, mesmo se a requisição ignorar o site/app. Máscaras são normalizadas antes da persistência. Profissional continua com CPF obrigatório; responsável pode enviar `null`, omitir ou deixar em branco conforme o contrato anterior. Contas legadas não são excluídas ou bloqueadas por esse novo requisito.

**Não se deve chamar um número de falso só por uma falha de validação.** A mensagem é “CPF inválido. Confira o número informado.” Um CPF gerado com dígitos válidos pode não ter sido emitido ou pertencer a outra pessoa. O endpoint informa `cadastroConsultado:false` e as telas explicam essa limitação. Não se enviam CPFs a serviços terceiros.

Confirmar existência/situação cadastral exige um provedor oficial contratado, como a [Consulta CPF da Serpro](https://centraldeajuda.serpro.gov.br/duvidas/pt/avisos/avisoconsultacpfv3/), credenciais e adequação aos dados exigidos pelo contrato atual. Essa consulta não foi ativada, pois o projeto não fornece essas credenciais nem todos os campos exigidos. Verificar titularidade é uma etapa adicional. Não substituir essa verificação por APIs que só repetem o cálculo de dígitos.

## Proteção da recuperação

- Código aleatório criptográfico de 8 dígitos, validade de 15 minutos, até 5 tentativas, uso único. Reenvio após 60 segundos substitui o código anterior.
- Persistência somente do HMAC-SHA256 do código e do e-mail, usando `JWT_SECRET` com separação de contexto. O código em claro existe apenas em memória para envio; não entra em logs nem em respostas da API.
- Bloqueio de linha por conta serializa reenvio e consumo, inclusive solicitações simultâneas. As tentativas incorretas são confirmadas no banco, mesmo com resposta `400`.
- Senhas seguem as regras do cadastro: ao menos 8 caracteres, maiúscula, minúscula, número e símbolo, com limite BCrypt de 72 bytes UTF-8.
- Cada alteração incrementa a versão de credenciais e invalida JWTs anteriores em todos os dispositivos. JWTs legados sem versão continuam aceitos somente enquanto a versão da conta for zero. O formato de login continua `{token,email}`.
- Envio assíncrono após commit: a duração da chamada ao provedor não revela a existência da conta. Fila limitada a 50 tarefas, até 2 trabalhadores. Falhas não expõem endereço/código/chave; o usuário precisa solicitar novo código após corrigir a causa. A fila não é durável: um reinício antes do envio pode exigir nova solicitação.
- Limites adicionais por instância e por janela de 15 minutos: 3 solicitações por e-mail, 20 por origem, 15 confirmações por e-mail, 30 por origem. A origem vem do endereço remoto fornecido pelo servidor; o código não confia diretamente no header X-Forwarded-For. Se houver várias instâncias ou proxy agregando IPs, configure limitação centralizada na borda/Redis e os proxies confiáveis antes de ampliar o tráfego.

## Testar

```bash
bash gradlew test
bash gradlew bootJar
docker build -t tcc-backend .
```

Os testes cobrem os dois perfis, destinatário do banco, enumeração por resposta, código incorreto, expiração, senha fraca, reenvio, consumo concorrente, revogação dos JWTs e CPF no cadastro/API. H2 é o padrão local. O workflow do repositório executa também PostgreSQL, migração repetida e Docker. Nenhum segredo real é necessário nos testes; e-mails são simulados. O mock-maker de testes usa subclasses, evitando anexação automática de agentes JVM em ambientes restritos.

## Reversão e melhorias futuras

Para suspender apenas o envio, remova `RESEND_API_KEY`. Preserve tabelas/colunas e versões. Não reverta só o backend para uma versão que ignore `versaoCredenciais`: isso revalidaria tokens revogados; em rollback, rotacione `JWT_SECRET` e peça novo login a todos.

Melhorias futuras: fila durável com retentativas e métricas sanitizadas, limitador compartilhado entre instâncias, confirmação de e-mail no cadastro e contratação da consulta cadastral oficial, se exigida pelo projeto. Não confundir essas melhorias com funcionalidades já implantadas.
