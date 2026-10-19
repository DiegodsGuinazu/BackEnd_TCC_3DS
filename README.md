# BackEnd_TCC_3DS

Backend Spring Boot do EspectroCare, compartilhado pelo site e aplicativo.

- [Contrato de autenticação e APIs protegidas](docs/INTEGRACAO_SITE_APP.md)
- [Configuração de segurança](SECURITY.md)

Requisitos: Java 21 e Gradle Wrapper.

```bash
bash gradlew test bootJar --no-daemon
```

## Recuperação de senha sem domínio próprio

O envio por Gmail API é configurado exclusivamente no backend. Veja [o guia do Google Cloud e Render](docs/GMAIL_RECUPERACAO_SENHA.md). Compatível com o site/app alinhados ao PR #7; não exige outra migração do banco.
