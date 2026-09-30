# Configuração de segurança

## Chave de assinatura JWT

Configure `JWT_SECRET` como variável de ambiente no serviço de deploy e no
ambiente de desenvolvimento. A aplicação exige essa variável e não utiliza
uma chave padrão. Não inclua seu valor no Git ou em arquivos compartilhados.

Como a chave anterior foi versionada, substitua-a por uma chave nova e aleatória
antes do próximo deploy. Uma opção para gerar o valor localmente é
`openssl rand -hex 32`. Cadastre o resultado diretamente no gerenciador de
segredos ou nas variáveis de ambiente do serviço de deploy.

A rotação invalida os tokens anteriores: os usuários precisarão autenticar-se
novamente. Remover a chave dos arquivos atuais não a remove do histórico Git;
trate a chave antiga como comprometida e não a reutilize em nenhum serviço.

## Contêiner

A imagem final executa o Java com o usuário sem privilégios `app`.

## Validação

Com Java 21 e as variáveis de ambiente configuradas, execute:

```bash
bash gradlew test build --no-daemon
```

Os testes existentes carregam o contexto Spring e exigem um banco de testes.
Use um banco isolado, nunca as credenciais ou o banco de produção.
