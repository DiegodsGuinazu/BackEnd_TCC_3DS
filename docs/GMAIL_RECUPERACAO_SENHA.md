# Envio de recuperação por Gmail — EspectroCare

Este guia usa a conta remetente `espectrocare@gmail.com`. O backend envia pela Gmail API em HTTPS, sem SMTP e sem exigir domínio próprio. A conta autoriza o backend uma vez; os destinatários recebem o código normalmente, sem conectar suas contas Google ao projeto.

O PR deve ser aplicado **depois do PR #7**, já integrado à `main`. Rotas, corpos e respostas de recuperação do #7 são preservados; o site e o app alinhados a ele continuam compatíveis. **Não há nova migração de banco nem necessidade de alterar frontend/Android.**

## 1. Criar o projeto e habilitar Gmail API

1. Entre no [Google Cloud Console](https://console.cloud.google.com/) usando a conta da equipe que controla `espectrocare@gmail.com`.
2. No seletor de projetos, escolha **Novo projeto**, com nome `EspectroCare Email`.
3. Com esse projeto selecionado, abra [Gmail API](https://console.cloud.google.com/apis/library/gmail.googleapis.com) e clique em **Ativar**.
4. Abra **Google Auth Platform**. Dependendo da interface, a entrada aparece como **APIs e serviços → Tela de consentimento OAuth**.
5. Configure a identificação do aplicativo como `EspectroCare`, com e-mail de suporte/contato da equipe. Para uma conta Gmail pessoal, escolha público **External/Externo**.
6. Em **Audience/Público**, enquanto estiver em **Testing/Teste**, adicione **somente a conta remetente** `espectrocare@gmail.com` como usuário de teste. Os responsáveis/profissionais que apenas recebem e-mail não são usuários OAuth e não precisam ser incluídos.
7. Em **Data Access/Acesso a dados**, confira os escopos:
   - `https://www.googleapis.com/auth/gmail.send` — enviar mensagens;
   - `openid` e `email` — confirmar que a autorização é da conta remetente correta.

Não pedir escopos de leitura, exclusão ou gerenciamento da caixa de entrada. O helper abaixo só pede envio e identidade básica. É a autorização da equipe para envio, e não um recurso de login Google dos usuários do site/app.

## 2. Criar o cliente OAuth

1. Em **Google Auth Platform → Clients/Clientes**, clique em **Create client/Criar cliente**.
2. Tipo: **Web application/Aplicativo da Web**.
3. Nome: `EspectroCare envio Gmail`.
4. Em **Authorized redirect URIs/URIs de redirecionamento autorizados**, adicione exatamente:

   ```text
   http://127.0.0.1:8765/oauth2/callback
   ```

   Esse endereço é do computador da pessoa que fará a autorização. Não é a URL do Render e não publica um callback no backend.
5. Crie o cliente e baixe o JSON das credenciais. Se mudar o callback depois, baixe o JSON atualizado. O segredo do cliente deve ser guardado neste momento.
6. Guarde o JSON **fora do repositório**, por exemplo em `%USERPROFILE%\EspectroCare-segredos\client_secret_gmail.json`. Não enviar ao chat, Git, frontend ou app.

## 3. Autorizar a conta no seu computador

Requisitos: Python 3.10+ instalado e navegador disponível. O script usa somente a biblioteca padrão do Python. Nenhum envio de e-mail é feito por ele.

Atualize a cópia local do backend para a versão deste PR. No terminal PowerShell do VS Code, na raiz que contém `gradlew.bat`:

```powershell
python --version
python scripts/autorizar_gmail.py --client-json "$env:USERPROFILE\EspectroCare-segredos\client_secret_gmail.json"
```

Se o Windows usa o launcher `py`, substitua `python` por `py` nos comandos.

O script abre o navegador. Selecione `espectrocare@gmail.com`, confira o nome do projeto e as permissões e autorize. A página local orientará a voltar ao terminal. Ele verifica o e-mail via Google antes de gravar qualquer token. Conta errada, state inválido, acesso offline ausente ou permissão negada não produzem arquivo de configuração.

Ao terminar, será criado:

```text
%USERPROFILE%\EspectroCare-segredos\gmail-render.env
```

Abra esse arquivo somente no seu computador e copie os valores para o Render, sem compartilhá-los no chat. O script não mostra tokens/segredos no terminal, não registra a URL do callback e não sobrescreve um arquivo existente. Em sistemas Unix o arquivo recebe permissão 0600; no Windows, mantenha-o em sua pasta pessoal com acesso restrito à equipe autorizada.

Se precisar autorizar novamente, use um nome novo para preservar o arquivo anterior até confirmar a configuração:

```powershell
python scripts/autorizar_gmail.py --client-json "$env:USERPROFILE\EspectroCare-segredos\client_secret_gmail.json" --output "$env:USERPROFILE\EspectroCare-segredos\gmail-render-novo.env"
```

A autorização espera por até cinco minutos. A porta local 8765 precisa estar livre. Se aparecer `redirect_uri_mismatch`, conferir o callback exato no cliente Web e baixar novamente o JSON. Não colar códigos OAuth, tokens ou o JSON no terminal compartilhado/chat.

## 4. Configurar o backend no Render

No serviço do **backend → Environment**, cadastre os cinco valores do arquivo gerado:

| Variável | Valor |
| --- | --- |
| `EMAIL_PROVIDER` | `gmail` |
| `GMAIL_CLIENT_ID` | ID do cliente OAuth |
| `GMAIL_CLIENT_SECRET` | Segredo do cliente OAuth |
| `GMAIL_REFRESH_TOKEN` | Token de renovação da autorização da conta |
| `GMAIL_SENDER` | `espectrocare@gmail.com` |

Salve e refaça o deploy com a versão deste PR. Mantenha `JWT_SECRET`, banco Neon e CORS existentes. Estes valores não vão para o Neon, HTML, JavaScript ou APK. Não usar uma API key simples do Google nem a senha do Gmail nesses campos.

No modo Gmail, `RESEND_API_KEY` e `MAIL_FROM` não participam do envio. Podem permanecer para um eventual retorno à Resend; não é preciso configurar Resend para o Gmail funcionar. Sem `EMAIL_PROVIDER=gmail`, o padrão continua sendo `resend`, preservando instalações anteriores.

Configuração Gmail incompleta ou provedor desconhecido deixa apenas a recuperação indisponível (503); não impede o backend de iniciar. A opção Gmail não troca silenciosamente para Resend quando há erro.

## 5. Testing e funcionamento contínuo

**Importante para a operação:** o Google emite refresh tokens que expiram em **sete dias** para aplicativos externos em **Testing** que usam escopos Gmail. Mesmo com o access token renovado automaticamente, esse limite do refresh token continua valendo.

Testing serve para validar a configuração. Antes de depender do envio contínuo, revise **Audience/Público → Publishing status/Status de publicação** no Google Auth Platform e siga o processo exibido para publicar/validar o aplicativo. Se o Google solicitar verificação, documentos, domínio ou bloquear a autorização, conclua os requisitos aplicáveis; este código não elimina essas exigências. A verificação não deve ser considerada automaticamente aprovada. Não prometa operação permanente com um token de teste.

Após mudar o status, gere uma nova autorização com o script e atualize o `GMAIL_REFRESH_TOKEN` no Render. Tokens também podem ser revogados pela conta Google, por mudança de senha com escopos Gmail, políticas ou outros eventos; autorize novamente nesses casos. Aplicam-se limites de envio da conta e quotas da Gmail API. Para maior volume/uso público, reavaliar o provedor e a operação.

## 6. Validar sem alterar os clientes

1. Confirme o deploy do backend com este PR.
2. Com uma conta de responsável e uma de profissional com e-mails controlados pela equipe, solicite recuperação pelo login e pelo perfil do site/app atualizado.
3. Confira remetente, destinatário da conta, código de oito dígitos, caixa de entrada e spam. O destinatário continua sendo escolhido no banco; não é o remetente.
4. Confirme a nova senha, faça login novamente e confira que a senha e o JWT antigos deixaram de funcionar. Expiração, tentativas, uso único e limites continuam sendo os do #7.
5. Verifique que a opção do perfil continua fazendo `POST /api/conta/recuperacao-senha` sem e-mail no corpo. Não há rota OAuth pública adicionada no Render.

As respostas de solicitação continuam genéricas para evitar enumeração de contas. Se o e-mail não chegar, conferir as cinco variáveis, a autorização/permissões, o status de publicação e as quotas no Google Cloud. Logs do backend exibem só aviso genérico, sem corpo do provedor, destinatário, código ou token. Não habilitar logs de conteúdo HTTP contendo credenciais.

## Implementação, testes e decisões

- `Service/GmailRecuperacaoService.java`: refresh OAuth por HTTPS, cache do access token até um minuto antes da expiração, serialização da renovação, MIME UTF-8/base64url e envio para a Gmail API. URLs fixas, sem redirecionamentos, conexão de 10 s e leitura de 15 s. Recusa CR/LF em endereços e código fora do formato. Falhas externas são sanitizadas; 401 invalida o cache sem repetir automaticamente um envio.
- `Service/EmailRecuperacaoService.java`: seleciona explicitamente `gmail` ou `resend`, preservando o contrato usado pela recuperação e pelo listener após commit.
- `application.properties`: configuração opcional por variáveis; ausência não derruba login/cadastro.
- `scripts/autorizar_gmail.py`: bootstrap local com PKCE S256, state aleatório validado, bind somente em 127.0.0.1 e confirmação da conta Google. Secrets apenas em arquivo privado fora do repo. O script não envia e-mail.
- `.gitignore`: impede versionamento acidental dos nomes usuais desses arquivos privados.

Não foram adicionadas dependências: o backend usa `RestClient` já existente para dois endpoints HTTPS documentados (renovação e envio). O fluxo de autorização ocorre localmente, sem implementar um login Google público no servidor. A equipe precisa manter o pequeno adaptador e acompanhar a documentação do Google. A biblioteca oficial de autenticação é uma alternativa para ampliar a integração futuramente.

```powershell
# Testes do helper: não acessam Google, não usam credenciais reais.
python -m unittest discover -s tests -v
# Testes Java e geração de JAR.
.\gradlew.bat test bootJar
# Build Docker.
docker build -t tcc-backend .
```

Testes novos verificam seleção de provedor, MIME/destinatário, injeção de cabeçalho, reutilização/renovação e invalidação do access token, erros sanitizados, PKCE/state, conta correta, escopos, callback e gravação privada sem sobrescrita. A CI existente executa Java em H2/PostgreSQL e build Docker. Não há teste real do Google sem as credenciais privadas e o consentimento da equipe.

## Desfazer

Para voltar à Resend, usar `EMAIL_PROVIDER=resend` com `RESEND_API_KEY` e `MAIL_FROM` válidos. Isso exige remetente/domínio autorizado na Resend. Para suspender recuperação, remover a configuração do provedor selecionado; login/cadastro continuam funcionando.

Se abandonar a integração Gmail, revogue o acesso do aplicativo nas configurações de segurança da conta Google e remova as variáveis Gmail no Render. Preserve o banco e as versões de credenciais que revogam JWTs; este PR não altera o esquema. O rollback do adaptador não exige mesclar o PR #6.

Referências oficiais:
- https://developers.google.com/workspace/gmail/api/guides/sending
- https://developers.google.com/identity/protocols/oauth2/web-server
- https://developers.google.com/identity/protocols/oauth2#expiration
- https://developers.google.com/workspace/gmail/api/auth/scopes
