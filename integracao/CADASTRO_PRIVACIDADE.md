# Aplicar correções do app e do site

A integração do GitHub recusou escrita nos repositórios dos outros proprietários (403). Estes patches contêm todas as alterações e arquivos novos, incluindo testes. A API é entregue neste PR; o app e o site ainda precisam receber os patches.

## Aplicativo — GiovanniBarattaL/NeuroHelp

Base: `f99d71d073be992c003abefc7460de7774dda964` (confira a base registrada no PR antes de aplicar).

```sh
git switch -c fix/cadastro-privacidade-app
git apply --check /caminho/android-cadastro-privacidade.patch
git apply /caminho/android-cadastro-privacidade.patch
bash gradlew testDebugUnitTest
bash gradlew connectedDebugAndroidTest
```

O patch alinha formacao, preenche a privacidade dos dois perfis, mostra erros por campo, valida senha antes da confirmação, reforça 11 dígitos no telefone e aponta LGPD para a página da ANPD. Não implementa salvamento das edições do perfil.

## Site — kaykekkj/Espectro-Care

Base: `99946bfd336a8b30481f4c5f2b5bca9d685c2367`.

```sh
git switch -c fix/erros-cadastro
git apply --check /caminho/app-cadastro-privacidade.patch
git apply /caminho/app-cadastro-privacidade.patch
node --test tests/cadastro-validacao.test.cjs tests/crm-verificacao.test.cjs
```

O limite do telefone já existia no site; o teste cobre essa regra. O patch adiciona diagnóstico de validação da API sem exibir valores rejeitados ou detalhes internos.

Publicar a API antes de distribuir o app atualizado. Contas antigas continuam válidas, mas endereços que a API anterior descartou não podem ser recuperados automaticamente.
