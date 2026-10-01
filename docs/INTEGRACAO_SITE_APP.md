# Autenticação e áreas protegidas — site e aplicativo

O site e o aplicativo usam a mesma API, os mesmos endpoints e o mesmo JWT.
Esta etapa prepara o backend. As páginas e telas dos clientes ainda precisam
consumir as novas rotas. A Home hospedada no frontend continua pública.

## Rotas e acesso

| Método | Rota | Acesso | Resposta/finalidade |
|---|---|---|---|
| POST | `/auth/login` | Público | `{ "token": "...", "email": "..." }` |
| POST | `/cadastro/profissional` | Público | Cria profissional; sucesso em texto, como antes |
| POST | `/cadastro/responsavel` | Público | Cria responsável; sucesso em texto, como antes |
| GET | `/api/auth/me` | Ambos os perfis, com JWT | Identifica o usuário logado |
| GET | `/api/profissionais` | Ambos os perfis, com JWT | Lista dados de apresentação dos profissionais cadastrados |
| GET | `/api/profissionais/{id}` | Ambos os perfis, com JWT | Detalhe do profissional |
| GET | `/api/aprendizagem` | Ambos os perfis, com JWT | Lista resumos dos conteúdos selecionados pela equipe |
| GET | `/api/aprendizagem/{id}` | Ambos os perfis, com JWT | Conteúdo completo de um artigo selecionado |
| GET/PUT | `/api/perfil` | Apenas profissional, com JWT | Consulta/atualiza seu próprio perfil |
| GET/PUT | `/api/perfil-responsavel` | Apenas responsável, com JWT | Consulta/atualiza seu próprio perfil |
| Qualquer | `/publicacao/**` | Bloqueado | Publicação aguarda autorização da equipe |
| Qualquer | `/h2-console/**` | Bloqueado | Console desativado |
| POST | `/usuarios/autenticar` | JWT, legado | Booleano; não usar como login, pois não gera token |
| Qualquer | Demais rotas | JWT por padrão | Não ficam públicas automaticamente |

Preflight `OPTIONS` pode passar sem JWT dentro da política de CORS. `/error`
permanece disponível para processamento de erros; não é um endpoint de dados.

Os DTOs `PerfilResponseDTO` e `PerfilResponseRespDTO` continuam destinados ao
próprio usuário. O CPF permanece nessas respostas privadas. Não reutilizá-los
para uma listagem de outras pessoas. O telefone é divulgado na listagem de
profissionais junto dos dados de apresentação; antes do lançamento, confirmar
com a equipe se é um telefone destinado a contato profissional e se esse uso
está claro no cadastro.

## Contrato comum e exemplos

Login:

```http
POST /auth/login
Content-Type: application/json

{"email":"usuario@exemplo.com","senha":"senha-do-usuario"}
```

O sucesso continua retornando `token` e `email`. Credenciais incorretas
continuam retornando `401` com texto `Email ou senha inválidos.` por
compatibilidade. O cliente deve verificar o status antes de chamar `json()`.

Cada requisição protegida deve incluir:

```http
Authorization: Bearer TOKEN_RECEBIDO_NO_LOGIN
```

`GET /api/auth/me`:

```json
{"id":"1","nome":"Exemplo","email":"usuario@exemplo.com","tipoPerfil":"PROFISSIONAL"}
```

O outro valor de `tipoPerfil` é `RESPONSAVEL`. Os IDs de usuários são strings,
como nos DTOs de perfil existentes. Eles identificam registros dentro de cada
tipo de perfil; não representam um ID global entre as duas tabelas.

`GET /api/profissionais`:

```json
[{"id":"1","nome":"Profissional","bio":"Apresentação","telefone":"11999999999","estado":"SP","numRegistro":"REG-EXEMPLO"}]
```

CPF, email, senha e authorities não são expostos nessas rotas. A listagem
consulta `UserProf`, ordenada por nome e ID. Não há aprovação ou verificação
de credenciais profissionais implementada: estar listado não comprova registro.

`GET /api/aprendizagem`:

```json
[{"id":1,"titulo":"Artigo da equipe","dataCriacao":"2026-09-30T12:00:00","dataAtualizacao":null,"categorias":["TEA"]}]
```

`GET /api/aprendizagem/1` acrescenta `conteudoHtml` ao objeto do artigo.
Não retorna entidades, relações JPA ou anexos. As datas usam a modelagem
existente `LocalDateTime`, sem informação de fuso. Lista vazia retorna `200 []`;
ID inexistente ou conteúdo não selecionado retorna `404`.

Sem JWT válido, a segurança retorna:

```json
{"status":401,"mensagem":"Autenticação necessária ou token inválido."}
```

Perfil errado ou tentativa de publicação retorna `403` com `status` e
`mensagem`. O JWT dura duas horas, calculadas em UTC com `Instant`,
independentemente do fuso do servidor. O usuário precisa existir no banco.
Novos JWTs incluem `tipoPerfil`, para que o mesmo email em tabelas distintas
não troque a identidade da sessão. Tokens anteriores sem essa informação
continuam funcionando quando o email existe em apenas uma tabela. Um email
legado duplicado nas duas tabelas bloqueia login e tokens antigos até a equipe
corrigir a duplicidade; novos tokens tipados não trocam de perfil.
Ainda não há refresh token ou revogação individual no logout: o cliente remove
seu token local, mas uma cópia válida continua utilizável até expirar.

Os campos já usados no cadastro foram preservados. Agora são recebidos por
DTOs específicos e mapeados para uma entidade nova; um `id` enviado pelo
cliente não pode sobrescrever uma conta existente. Email, CPF e senha são
obrigatórios e o email precisa ter formato válido; dados inválidos retornam `400`.
O cadastro verifica se o email já existe em qualquer um dos tipos e retorna
`409` com mensagem genérica em caso de conflito. Violações de unicidade no
banco também retornam `409`, sem expor a constraint ou o SQL.

## Integração nos clientes

No site, a Home fica pública. Ao abrir Profissionais ou Aprendizagem, validar
o token com `/api/auth/me` ou consultar diretamente a API protegida. Em `401`,
limpar a autenticação local e redirecionar para Login/Cadastro; depois do login,
voltar para a área solicitada. Em `403`, exibir falta de permissão, sem repetir
o login automaticamente. Um erro de rede/`5xx` deve permitir nova tentativa.

No Android, usar o mesmo cabeçalho no cliente HTTP e encaminhar `401` para
a tela de autenticação. Usar armazenamento privado apropriado para o token.
Nenhum segredo JWT pertence ao JavaScript ou ao APK.

O backend protege os dados, não o HTML hospedado separadamente. Os textos e
profissionais fixos que hoje estão no HTML continuam públicos até serem
substituídos por consumo da API. Não considerar o redirecionamento por JS
uma proteção de conteúdo embutido. Renderizar HTML dos artigos apenas após
sanitização com uma política de elementos permitidos; o DTO não sanitiza HTML.

Na `main` do repositório NeuroHelp inspecionada nesta etapa há frontend web;
não foi encontrado projeto Android. O código Android não foi alterado.
Também existem problemas prévios no JavaScript de login/cadastro e URLs
`localhost:8080`; a integração desses arquivos é uma etapa posterior.

## Seleção de conteúdos e compatibilidade do banco

`Postagem` foi reaproveitada, com `publicadoAprendizagem` inicialmente `false`.
A coluna física padrão do Spring/Hibernate é `publicado_aprendizagem`.
As postagens existentes e novas ficam fora de Aprendizagem por padrão.
Categorias existentes também foram reaproveitadas; nenhuma entidade de
artigo duplicada foi criada.

Com o atual `ddl-auto=update`, Hibernate adiciona a coluna booleana com
default `false` no primeiro início. Em um ambiente que controla o schema
por migração, aplicar antes do deploy, com backup e em banco de homologação:

```sql
ALTER TABLE postagem
ADD COLUMN IF NOT EXISTS publicado_aprendizagem boolean NOT NULL DEFAULT false;
```

Somente a equipe, por um processo controlado de administração do banco,
deve selecionar conteúdos revisados com essa coluna `true`. Não foi criado
um endpoint de escrita nem uma credencial administrativa nesta etapa.
`/publicacao/salvar` foi bloqueado porque anteriormente qualquer usuário
autenticado podia tentar publicar e não existe autorização de equipe.
Se algum cliente usa essa rota, ele passará a receber `403`.

## Configuração e validação

Java 21, Gradle Wrapper e um banco isolado são os pré-requisitos. Produção
continua exigindo `JWT_SECRET` e as três variáveis `SPRING_DATASOURCE_*`.
Testes usam segredo fictício e H2 em memória; não usam o banco de produção.

Definir `CORS_ALLOWED_ORIGINS` com origens exatas separadas por vírgula,
sem caminhos ou barra final. O padrão inclui
`https://neuro-help-psi.vercel.app`, localhost nas portas 3000, 5500 e 8080,
e `127.0.0.1:5500`. Se o domínio/porta do site for outro, adicioná-lo antes
da integração. Em produção, preferir apenas os domínios realmente usados.
Android normalmente não envia `Origin` e usa o mesmo JWT. CORS não substitui
autenticação. O console H2 e o log SQL foram desativados em produção.

```bash
bash gradlew test bootJar --no-daemon
docker build -t tcc-backend .
```

O workflow `Validar backend` repete os testes com H2 e PostgreSQL 16 isolado,
em fuso UTC, e valida o build Docker. Ele não publica uma imagem nem faz deploy.
Os testes cobrem autenticação, tokens inválidos/expirados, usuário removido,
ambos os perfis, DTOs sem dados privados, perfis próprios, `403`, artigos
não selecionados, `404`, CORS, cadastro e prevenção de sobrescrita por ID.

Para desfazer, reverter o commit antes de um deploy. A coluna adicionada
pode permanecer no banco sem afetar a versão anterior; não é necessário
apagá-la. Uma reversão também restaura a política de segurança anterior.

## Evolução prevista

Definir um perfil de equipe/administrador e permissões para publicar artigos;
depois substituir os conteúdos fixos dos clientes pelo consumo da API.
Antes de crescer a base, adicionar paginação, migrações versionadas e
revisar quais dados de contato devem ser publicados. As tabelas de
profissionais/responsáveis continuam separadas, com constraint de email único
apenas dentro de cada tabela. A verificação no cadastro impede duplicidade
entre tipos em requisições sequenciais; unificar a identidade ou impor
unicidade global no banco é a evolução necessária para cobrir também
cadastros simultâneos. A validação tipada do JWT impede troca de conta mesmo
se uma duplicidade desse tipo acontecer. Não foi feita migração de contas.


## Atualização de cadastro e perfis

Os contratos existentes de autenticação foram preservados: \`POST /auth/login\`
continua retornando somente \`token\` e \`email\`, e o tipo do usuário continua
sendo obtido por \`GET /api/auth/me\`. Isso evita quebra nos clientes Android e
web que já consomem o login atual.

Os cadastros agora aceitam os campos aditivos \`cidade\` para ambos os perfis e
\`formacao\` para profissional. Clientes antigos podem continuar omitindo esses
campos. CPF e telefone são normalizados para somente dígitos antes da
persistência. O CPF profissional continua obrigatório. Para responsável, \`cpf\`
pode ser \`null\`; valores vazios também são normalizados para \`null\`.

A senha de novos cadastros deve ter no mínimo oito caracteres e incluir letra
maiúscula, letra minúscula, número e caractere especial. Isso não altera senhas
já persistidas nem o contrato de login.

### Alteração necessária no PostgreSQL de produção

A entidade de responsável não exige mais \`NOT NULL\` no CPF, mas
\`spring.jpa.hibernate.ddl-auto=update\` não deve ser considerado suficiente
para remover uma constraint já existente. Antes do deploy, conferir o nome real
da tabela e da coluna no banco de produção. Se a tabela for de fato
\`user_resp\` e a coluna \`cpf\`, a alteração esperada é:

\`\`\`sql
ALTER TABLE user_resp
ALTER COLUMN cpf DROP NOT NULL;
\`\`\`

Executar somente depois de confirmar a estrutura com o catálogo do PostgreSQL e
preferencialmente com backup/homologação. A constraint de unicidade deve ser
mantida: PostgreSQL permite vários valores \`NULL\` em uma coluna \`UNIQUE\`.

### Compatibilidade

As mudanças são aditivas: nenhum endpoint foi renomeado, nenhum campo antigo foi
removido e os códigos de sucesso do cadastro/login permanecem os mesmos. DTOs
de perfil e de listagem de profissionais passam a poder incluir \`cidade\` e
\`formacao\`; clientes que ignoram campos desconhecidos permanecem compatíveis.
