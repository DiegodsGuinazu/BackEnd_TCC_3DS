# CPF e recuperação de senha — site e Android

## Comportamento

O cadastro de responsáveis e profissionais agora rejeita CPF com dígitos verificadores incorretos, tamanho inválido ou sequência repetida (400, texto “CPF inválido”). CPF opcional do responsável continua sendo normalizado para `null`. Os dados antigos não são revalidados nem removidos.

Esta implementação valida matematicamente o documento, sem consulta externa, custo ou envio de CPF a terceiros. **Não comprova existência na Receita, situação cadastral ou titularidade.** Não chamar um CPF de “falso” apenas porque houve erro de digitação. A consulta oficial Serpro exige contratação, credenciais e, na versão 3, CPF + data de nascimento. Essa integração externa permanece pendente; não foi criada uma simulação de consulta cadastral.

A recuperação envia um código numérico de 8 dígitos ao endereço da conta. Prazo de 15 minutos, cinco tentativas, um único uso e novo pedido somente após 15 minutos. Um pedido novo não aumenta o orçamento de tentativas do código ativo. Não é necessário informar a senha antiga.

O envio do perfil usa exclusivamente o e-mail carregado do banco a partir do JWT. O corpo enviado pelo cliente não escolhe destinatário. Login e cadastro mantêm seus contratos. Depois da troca, JWTs anteriores são recusados; entrar novamente nos dispositivos. JWTs legados continuam válidos até a primeira troca de senha ou expiração normal.

## Ativar o envio no Render

O adaptador usa a API HTTPS do Resend, sem nova dependência. Requisições e códigos não são escritos nos logs.

1. No provedor Resend, configure e verifique um domínio remetente e crie uma chave de envio. O domínio de testes não permite enviar livremente para usuários; confirme as restrições do provedor.
2. Nas variáveis de ambiente do **backend** Render, configure `RESEND_API_KEY` com a chave privada e `MAIL_FROM` com um remetente autorizado, por exemplo `EspectroCare <seguranca@seu-dominio.com.br>`.
3. Nunca coloque a chave no Git, site ou Android. Não altere `JWT_SECRET` durante a atualização sem planejar a invalidação de sessões e códigos.
4. Execute `docs/sql/2026-10-19-recuperacao-senha.sql` no PostgreSQL antes de publicar o backend. É idempotente. Com `ddl-auto=update`, Hibernate também adiciona os campos, mas o script deixa a alteração explícita para revisão.
5. Publique o backend primeiro, depois o site e o app. Cadastre uma conta controlada com seu e-mail real e teste entrega na caixa de entrada/spam.

Sem as variáveis, login e cadastro funcionam; pedidos de recuperação retornam 503 com mensagem segura. A ausência dessas credenciais impede testar a entrega real neste trabalho. A equipe deve configurar as credenciais no Render; não é preciso compartilhá-las no chat.

O envio ocorre fora da resposta HTTP e após o commit, em fila limitada a 100 pendências e dois trabalhadores. Isso evita que o tempo do provedor revele se uma conta existe e que rollback dispare e-mail. A fila reside na memória: reinício, fila cheia ou rejeição do provedor podem impedir entrega; o usuário pode solicitar outro código após 15 minutos. Logs registram somente um aviso genérico nessas falhas. Para maior volume, evoluir para fila durável/outbox criptografada e limitar pedidos por origem no gateway. Não remover o limite por conta.

## Contratos

| Rota POST | Acesso | Corpo | Resultado |
|---|---|---|---|
| `/api/validacoes/cpf` | Público | `{"cpf":"529.982.247-25"}` | 200, `{"valido":true,"tipoValidacao":"DIGITOS_VERIFICADORES","mensagem":"..."}` |
| `/auth/esqueci-senha` | Público | `{"email":"pessoa@exemplo.com"}` | 202 genérico para conta presente, ausente, ambígua ou prazo ativo |
| `/api/conta/senha/solicitar` | JWT de responsável/profissional | Sem corpo | 202 genérico; destinatário vem do banco |
| `/auth/redefinir-senha` | Público | `{"email":"pessoa@exemplo.com","codigo":"01234567","novaSenha":"NovaSenha@123"}` | 200 ou 400; resposta JSON com `mensagem` |

Senha: mínimo de 8 caracteres, maiúscula, minúscula, número e símbolo; até 72 bytes UTF-8 por compatibilidade com BCrypt. Código, senhas e tokens nunca aparecem na URL ou na resposta. O banco guarda HMAC do código, expiração, tentativas e vínculo à conta. As entidades de usuário têm versão otimista para impedir que uma edição simultânea de perfil restaure a senha antiga.

## Validação e aplicação

- Java 21: `bash gradlew test bootJar`.
- Docker: `docker build -t tcc-backend .`.
- A CI existente executa H2, PostgreSQL e build Docker em PRs para `main`.
- Testes novos cobrem os dois perfis, CPF inválido, destinatário fixo, expiração, cinco tentativas, prazo de reenvio, troca de código, uso único, conta ausente, senha fraca, falta de configuração, entrega somente após commit, rollback e revogação de JWT.
- Teste manual: solicitar código no perfil, conferir o destinatário, sair e recuperar pelo login, trocar senha, conferir rejeição da senha antiga e da sessão antiga no outro cliente. Repetir para ambos os perfis. Solicitar código antes da troca e tentar reutilizá-lo depois: deve falhar.
- As fixtures antigas que aceitavam CPF com dígitos incorretos foram substituídas por exemplos matematicamente válidos.

## Desfazer e limitações

Reverter os commits de site e Android antes do backend. Manter as colunas/tabela adicionais evita destruição de dados. **Reverter o backend perde a revogação por `token_version`**; se houver necessidade de rollback após trocas de senha, rotacionar `JWT_SECRET` no Render para não reativar JWTs antigos (todos deverão entrar novamente). Não apagar tabelas de usuários.

Administradores continuam com o fluxo atual; este recurso atende responsáveis e profissionais. O app continua com seus campos atuais de cadastro; o CPF do responsável já era obrigatório na interface Android, enquanto o backend mantém a opção `null` usada pelo site.

Referências oficiais:
- https://resend.com/docs/api-reference/emails/send-email
- https://centraldeajuda.serpro.gov.br/duvidas/pt/avisos/avisoconsultacpfv3/
- https://www.gov.br/pt-br/servicos/obter-solucao-de-consulta-de-dados-de-cadastro-de-pessoa-fisica-cpf
