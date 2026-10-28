# Cadastro e consulta de privacidade

`GET /api/conta/privacidade`, com `Authorization: Bearer <token>`, retorna apenas a própria conta de profissional ou responsável. Não recebe ID de outra pessoa, não retorna senha, hash ou campos internos e usa `Cache-Control: no-store`. Sem autenticação retorna 401; administradores não usam essa rota.

Resposta: nome, email, cpf, telefone, estado, cidade, cep, bairro, logradouro, numero, formacao e numRegistro. Campos ausentes são nulos. Os dados de nascimento, sexo e dependente não eram coletados/persistidos pelo cadastro e não são inventados. Esta entrega preenche dados; não implementa edição ou upload de documentos.

O cadastro de ambos os perfis aceita opcionalmente cep (8 dígitos), bairro (120 caracteres), logradouro (160) e numero (20). A API continua aceitando o contrato do site, com `formacao`; `profissao` é um alias para compatibilidade com versões anteriores do app. O app atualizado envia `formacao`.

Erros de Bean Validation retornam HTTP 400 com `mensagem` e `campos`, por exemplo:

```json
{"mensagem":"Confira os campos indicados.","campos":{"telefone":"Telefone deve possuir 10 ou 11 dígitos."}}
```

Não são retornados valores rejeitados. Clientes só mostram mensagens conhecidas, e códigos de confirmação inválidos recebem orientação específica (422). Erros de duplicidade continuam genéricos.

## Publicação e validação

1. Aplicar `docs/sql/2026-10-28-endereco-cadastro.sql` no PostgreSQL se o ambiente não usa ddl-auto=update; as colunas são opcionais. Não há preenchimento retroativo de endereços descartados pelas versões anteriores.
2. Publicar a API antes do app atualizado; depois publicar o site e distribuir o app.
3. Executar `bash gradlew test` com Java 21; `PrivacidadeCadastroTest` cobre cadastro com payload do app, persistência, privacidade, autenticação e erros sem valores sensíveis.
4. No app executar `bash gradlew connectedDebugAndroidTest` em emulador/dispositivo. Testa filtro de telefone e mensagens de validação. Validar manualmente cadastro profissional/responsável, consulta do perfil e LGPD.
5. No site executar `node --test tests/cadastro-validacao.test.cjs`.

Nesta sessão os testes do site passaram. O Gradle local não baixou sua distribuição devido à restrição de rede; H2, PostgreSQL, Docker e o build Android não foram executados. Não houve publicação em produção.
