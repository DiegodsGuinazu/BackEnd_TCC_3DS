# Convites, Aprendizagem e fotos

## Aplicação

1. Faça backup do PostgreSQL e teste em uma cópia de homologação. Não use o banco de produção nos testes automatizados.
2. Aplique `docs/sql/2026-10-05-convites-admin.sql` com sua ferramenta PostgreSQL. O script é idempotente e adiciona somente duas tabelas. As colunas `foto_perfil` de profissionais e responsáveis já existiam no código de origem; confira sua presença em bases antigas.
3. Configure `JWT_SECRET`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` por variáveis de ambiente. Mantenha a chave JWT atual para preservar sessões; se estiver comprometida, rotacione-a e exija novo login.
4. Defina `FRONTEND_URL` como a raiz do site, por exemplo `https://espectro-care.onrender.com`, **sem** `/pages`, query string ou fragmento. Em produção use HTTPS. O padrão local é `http://localhost:5500`.
5. Defina `ADMIN_INVITE_TTL=PT24H` (duração ISO-8601). Aceita duração positiva até 7 dias, por exemplo `PT30M`, `PT12H` e `P2D`.
6. Configure `CORS_ALLOWED_ORIGINS` com as origens exatas do site e da homologação, separadas por vírgula. CORS não substitui autorização.
7. Depois da migração, use `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` em produção; `update` continua como padrão para compatibilidade com o ambiente atual. Para bases antigas, aplique também as migrações anteriores do projeto antes de usar `validate`.
8. Java 21: execute `./gradlew test` e `./gradlew bootJar` (`gradlew.bat` no Windows), publique a imagem/JAR pelo processo habitual e aplique a pasta frontend do ZIP ao repositório `kaykekkj/Espectro-Care`. Configure a API em `assets/js/api-config.js` ou pela chave local `espectrocare_api_base_url` durante a homologação.

## Primeiro convite: comando interno sem servidor HTTP

Execute no ambiente do backend, com as mesmas variáveis de banco, JWT, URL e validade. O comando inicia apenas o contexto de persistência e **não abre porta HTTP**. Só funciona enquanto não existir administrador.

Linux/macOS, em diretório privado já existente:

```sh
export ADMIN_INVITE_EMAIL='administrador@seu-dominio.com'
export ADMIN_INVITE_OUTPUT='/caminho/privado/primeiro-convite.txt'
./gradlew primeiroConvite --no-daemon
```

Windows PowerShell:

```powershell
$env:ADMIN_INVITE_EMAIL='administrador@seu-dominio.com'
$env:ADMIN_INVITE_OUTPUT='C:\diretorio-privado\primeiro-convite.txt'
.\gradlew.bat primeiroConvite --no-daemon
```

Em um ambiente que tenha apenas o JAR, inclusive a imagem Docker:

```sh
java -Dloader.main=br.com.neurohelp.tcc_backend.BootstrapConvite \
  -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

O arquivo de destino deve ser novo e o diretório deve existir. O comando não sobrescreve arquivos: configura `0600` em POSIX ou ACL exclusiva do operador no Windows. Não imprime o link. Se não puder proteger ou escrever o arquivo, falha; uma falha de escrita após a emissão revoga o convite. Execute com um operador autorizado a configurar as permissões. Não use saída de terminal, logs, diretórios públicos ou diretórios servidos pelo site como destino.

Abra o arquivo privado, compartilhe o link completo por canal seguro e apague esse arquivo após o uso. O token fica no fragmento `#token=...`, evitando logs de URL do servidor e Referer. A página remove o fragmento do histórico após lê-lo; para recarregar um cadastro ainda não concluído, abra novamente o link original. A validação usa POST e o token permanece somente na memória do navegador.

## Uso e revogação

- A pessoa abre o link, recebe o e-mail autorizado do backend e informa nome e senha forte. O backend compara novamente o e-mail, faz BCrypt e consome o convite somente na transação que salva a conta. SHA-256 armazena apenas o hash de um token aleatório de 256 bits.
- Um bloqueio pessimista de linha impede duas transações de consumir o mesmo convite. Expiração e revogação são verificadas também no cadastro, não apenas ao abrir a página.
- Faça login pelo formulário existente e abra `pages/admin.html`. Administradores podem gerar, listar e revogar convites. O link só é retornado na emissão; copie-o para compartilhamento manual. A listagem não retorna token, hash ou link.
- Revogue pela área administrativa. Antes de existir o primeiro administrador, uma revogação emergencial pode ser feita internamente pelo **ID** do convite: `UPDATE convite_admin SET revogado_em = CURRENT_TIMESTAMP WHERE id = <ID> AND utilizado_em IS NULL;`. Use parâmetro na ferramenta, sem consultar ou registrar hashes. Um convite consumido não pode ser revogado para desfazer a conta: revogação não equivale à exclusão de administrador.
- Cadastro público ignora campos desconhecidos como `role` e `tipoPerfil` e cria exclusivamente `UserResp` ou `UserProf`. Não há rota pública de inicialização ou atribuição de papel.
- JWT e `/api/auth/me` passam a reconhecer `tipoPerfil: ADMIN`; a autenticação carrega essa identidade do banco. JWT legado sem perfil continua restrito às identidades comuns existentes, sem elevar privilégios.

## Contratos acrescentados

| Endpoint | Acesso | Corpo / resposta |
|---|---|---|
| POST `/cadastro/admin/convite/validar` | Público com token de convite | `{token}` → `{email, expiraEm}` |
| POST `/cadastro/admin` | Público com convite válido | `{nome,email,senha,token}` → 201 e `mensagem` |
| POST `/api/admin/convites` | ADMIN | `{email}` → 201 `{id,link,expiraEm}` |
| GET `/api/admin/convites` | ADMIN | Lista `{id,email,expiraEm,utilizadoEm,revogadoEm}` |
| DELETE `/api/admin/convites/{id}` | ADMIN | 204, revogação idempotente de convite não consumido |
| POST `/api/aprendizagem` | ADMIN | `{titulo,conteudoHtml}` → 201, DTO de artigo existente |
| PUT `/api/aprendizagem/{id}` | ADMIN | `{titulo,conteudoHtml}` → DTO de artigo existente |
| DELETE `/api/aprendizagem/{id}` | ADMIN | 204 |
| GET `/api/profissionais/{id}/foto` | Público | JPEG normalizado ou 404; `Cache-Control: no-store` |

As respostas de profissionais ganham apenas `fotoPerfilUrl` opcional, relativo à raiz da API. `null` representa ausência de foto. Os campos antigos, inclusive telefone e `numRegistro`, permanecem no contrato para o Android. CPF, e-mail, senha, hash e convites não fazem parte dessa resposta. A listagem do site oculta telefone e registro; o perfil individual exibe ambos.

Leitura existente preservada: listagens de profissionais e Aprendizagem são públicas; detalhes continuam autenticados. Profissionais e responsáveis continuam sem poder publicar. Admin pode gerenciar qualquer postagem **publicada na Aprendizagem**, sem critério de autor. Rascunhos e outras áreas não ganham acesso administrativo. Admin não acessa os perfis privados de profissionais/responsáveis nem altera suas fotos.

Criação/edição recebem DTO limitado, validam título/conteúdo e sanitizam HTML no servidor com jsoup 1.23.2. Dependência justificada: análise estrutural de HTML e remoção de elementos/atributos executáveis; expressões regulares não oferecem essa garantia. Referências: [Safelist](https://jsoup.org/apidocs/org/jsoup/safety/Safelist), [versões oficiais](https://jsoup.org/news/). Edição preserva anexos/categorias; exclusão remove anexos e vínculos JPA, mantendo categorias compartilhadas. O modelo de anexo atual contém URLs, sem integração de exclusão de arquivos externos: objetos em armazenamento externo precisam de uma política de limpeza quando essa infraestrutura existir. Esta entrega não apaga arquivos por URLs arbitrárias.

Erros: 400 para entrada/convite inválido; 401 para ausência de autenticação; 403 para falta de permissão; 404 para artigo/foto ausentes; 409 para conflito de cadastro ou convite já consumido ao revogar. Mensagens JSON com `mensagem` nas rotas novas, sem conteúdo rejeitado, senha ou token. Convites inválidos, expirados, revogados e usados recebem a mesma mensagem para evitar exposição de estado a terceiros.

## Fotos e armazenamento

O contrato existente `GET/PUT /api/conta/foto`, com `{fotoPerfil: dataURL}`, é preservado. O destino vem exclusivamente da identidade JWT, nunca de ID/e-mail enviado pelo cliente. O servidor aceita JPEG/PNG até 128 KB e 1024×1024, verifica o conteúdo real e o MIME declarado, recorta/reencodifica em JPEG 256×256 e elimina metadados. O site e o Android aceitam uma imagem JPEG/PNG/WebP até 5 MB e 24 MP e a reduzem antes do envio.

As imagens ficam na coluna TEXT do usuário, dentro do PostgreSQL persistente: não dependem do filesystem efêmero do container. Mantenha banco externo ou volume PostgreSQL durável e backup incluindo essas colunas. Reiniciar o backend preserva fotos; recriar/apagar o banco não preserva. Avatares têm fallback quando não há imagem ou falha de carga. Após salvar, evento no site atualiza o ícone; respostas/arquivos usam `no-store`, e o app também desativa cache HTTP. Em outro dispositivo, a foto é recuperada novamente do banco no login.

## Android

Código adaptado em `GiovanniBarattaL/NeuroHelp`, entregue em `aplicativo/` no ZIP. `DiegodsGuinazu/NeuroHelp` é antigo e não foi usado para implementação.

- Cadastro, login, JWT e edição de perfil anteriores permanecem compatíveis. `fotoPerfilUrl` é opcional; versões antigas podem ignorá-lo. Não há transferência de JWT por URL ao abrir a Aprendizagem no navegador: o login do site continua separado.
- `ApiService` remove o log de payload/resposta e acrescenta leitura pública de profissionais e JPEG sem cache/redirecionamento, com tamanho limitado.
- Perfil próprio carrega dados reais, permite selecionar/salvar a foto e reapresenta-a ao abrir as telas. Ícones de perfil carregam a foto da conta autenticada.
- Cards/perfil de profissionais agora consultam IDs reais da API. Telefone/registro ficam no perfil. Avaliações e modalidades antes demonstrativas não são apresentadas como dados de banco, pois não existem endpoints para elas. Favoritar no perfil conserva o comportamento local preexistente; filtros, favoritos persistentes e avaliações continuam dependendo de contratos futuros, fora desta atualização.
- Gestão administrativa e convite continuam no site. O app reconhece a sessão ADMIN pelo backend, mas esta entrega não acrescenta um painel administrativo nativo.

Compile com o SDK Android configurado, JDK adequado ao Gradle e `./gradlew assembleDebug testDebugUnitTest`. Para apontar a homologação HTTPS: `-PESPECTROCARE_API_URL=https://sua-api-de-homologacao`. Instrumentados: `./gradlew connectedDebugAndroidTest` com emulador/dispositivo. Teste nos aparelhos a seleção de foto, retorno ao perfil, novo login e o acesso às fotos criadas no site.

## Validação e reversão

Execute `./gradlew test` no backend: inclui regressões dos cadastros/logins, perfis, fotos, leitura pública, convites e consumo simultâneo. Use dados fictícios em banco de testes. Faça a homologação com PostgreSQL e `docker build -t tcc-backend .` antes da produção. O relatório do ZIP distingue testes executados de verificações pendentes.

Para desfazer a aplicação, publique os commits anteriores dos três projetos. Mantenha as tabelas e fotos: a versão anterior simplesmente não usa administradores/convites. Não faça DROP para reverter uma publicação. Se um convite precisar ser invalidado, revogue-o; não reutilize o token. Se houver rollback após edição/exclusão de conteúdo, restaure os registros necessários do backup conforme procedimento do banco.

Melhorias futuras: limites de tentativa no gateway para login/validação/convites, auditoria sem segredos, paginação de convites/profissionais, entidade central de identidade para unicidade de e-mail entre tabelas, política de limpeza para objetos externos e avaliações/favoritos com contrato próprio. Nenhum desses itens concede acesso a dados privados.
