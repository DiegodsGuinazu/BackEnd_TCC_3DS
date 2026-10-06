# Aplicação do frontend e do Android

Os patches desta pasta guardam as alterações dos projetos associados sem publicar em seus repositórios. A autorização de publicação foi aplicada somente ao backend.

- Frontend: `kaykekkj/Espectro-Care`, base `e66aa4560cf3198b9472b43bb34a40572bde9708`.
- Android: `GiovanniBarattaL/NeuroHelp`, base `b17a1c550fd4a08ac4a52c1f6573a47db0c1755e`.
- Backend: base `2c8a06803e2d66eb620ae98f331410c484011dad`.

Em uma cópia de trabalho de cada projeto, com alterações locais protegidas por commit/backup:

```sh
git apply --check /caminho/para/frontend.patch
git apply /caminho/para/frontend.patch
```

No Android, use `aplicativo.patch` no lugar de `frontend.patch`. Se a verificação falhar porque a base mudou, resolva os conflitos comparando a lista de arquivos; não sobrescreva alterações atuais automaticamente. Os patches incluem arquivos novos e o pequeno PNG de teste, em formato Git binário.

O ZIP também contém as árvores completas dos três projetos, sem metadados Git, banco real, caches, node_modules ou segredos de ambiente. Consulte `docs/ADMINISTRACAO_E_FOTOS.md` para aplicação e `docs/RELATORIO_VALIDACAO.md` para verificações e pendências.
