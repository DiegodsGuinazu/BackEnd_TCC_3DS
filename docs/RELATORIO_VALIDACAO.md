# Relatório de validação — 05/10/2026

## Executado

- Backend Java 21: `gradlew test bootJar` aprovado; **55 testes**, zero falhas/erros. Inclui 46 regressões anteriores e 9 cenários adicionais.
- Convites: cadastro válido, hash persistido, e-mail vinculado, senha BCrypt, validação, inválido/expirado/revogado/reutilizado, senha fraca sem consumo e acesso restrito à emissão/revogação.
- Concorrência real entre duas transações H2: somente uma consome o convite e cria o administrador.
- Cadastro público com `role: ADMIN`/`tipoPerfil: ADMIN`: continua criando apenas responsável/profissional; não recebe permissões administrativas.
- Aprendizagem: admin cria/edita/exclui artigos existentes; responsável/profissional não ganham poderes administrativos; visitante recebe 401. Edição preserva anexos/categorias; exclusão remove anexos/vínculos e mantém a categoria compartilhada. HTML executável é removido.
- Fotos: upload/releitura do banco para os dois perfis, autorização da própria conta, tipo/conteúdo/tamanho/dimensões, MIME divergente, URL pública opcional, ausência de dados privados e resposta sem cache.
- Primeiro convite: comando no JAR, sem servidor HTTP, arquivo privado com ACL exclusiva no Windows; falha de escrita foi verificada e o convite correspondente foi revogado. Comando corrigido e executado com sucesso.
- Site + API + H2 persistente local: cadastro pela página de convite; login administrativo; geração/revogação; CRUD com confirmação cancelada/aceita; foto no ícone, card, perfil e gestão; atualização/novo login; controles por perfil; número ausente no card e presente no perfil; botão sem sublinhado e foco visível.
- Navegador Edge/Chromium: telas verificadas em **375 e 1280 px**, sem transbordamento; capturas inspecionadas. Erro preexistente de elemento ausente no script de mostrar senha corrigido sem alterar o formulário.
- Backend local reiniciado depois do upload: fotos recuperadas do banco persistente e URLs JPEG continuam sem cache.
- Android: `assembleDebug testDebugUnitTest assembleDebugAndroidTest` aprovado; **4 testes unitários**, zero falhas. APK e APK de testes instrumentados compilados. Avisos de APIs obsoletas já existentes permanecem.

Os testes utilizaram somente banco de memória e banco local exclusivo de verificação, com dados fictícios. Nenhum banco real foi acessado. A exclusão funcional foi testada somente em registros criados pelos testes.

## Pendências externas

- Não há PostgreSQL/serviço Docker disponível neste ambiente. A migração PostgreSQL e a imagem devem ser homologadas no ambiente do projeto antes da aplicação em produção.
- Não há aparelho/emulador Android conectado/configurado: os testes instrumentados foram compilados, mas **não executados**. Validar seleção de imagem e comportamento visual em aparelho real/emulador.
- Configurar URL definitiva do site, origem CORS, validade, segredos e banco PostgreSQL persistente. Aplicar frontend/aplicativo em seus respectivos repositórios e efetuar o deploy habitual. Publicação de código no backend não equivale a deploy validado.
- O modelo existente de anexos possui URLs; não há serviço de remoção de objetos externos. A consistência relacional é garantida; limpeza externa depende dessa infraestrutura.
- Não há contrato de avaliações/favoritos persistentes no Android. O comportamento local de favorito do perfil permanece; dados demonstrativos não são tratados como dados reais.

## Repetição

Instruções operacionais em `ADMINISTRACAO_E_FOTOS.md`; teste do navegador em `frontend/tests/README.md` no ZIP; Android em `aplicativo/docs/ATUALIZACAO_FOTOS.md`. A lista exata de arquivos alterados e os commits de origem acompanham o ZIP e `integracao/ARQUIVOS_MODIFICADOS.md` no backend.
