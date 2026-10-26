# CRM — conferência manual gratuita

Substitui o WS pago do PR #11. Não exige SEI, chave ou `CFM_CHAVE`. Não acessa endpoints privados, não automatiza CAPTCHA e não faz scraping. A equipe consulta gratuitamente o [portal público do CFM](https://portal.cfm.org.br/busca-medicos/) por nome, número e UF.

## Fluxo

1. Cadastro e edição validam somente o formato local de registros explicitamente CRM: UF válida, 1 a 7 dígitos diferentes de zero, sufixo `-P` opcional. Não consultam serviço externo. A confirmação de e-mail, CPF, senha e autenticação continuam com as regras existentes.
2. O profissional vê o aviso de CRM não verificado ao lado do nome na Minha Área. Abaixo do número fica **Validar CRM**, com explicação de que solicita conferência pela equipe.
3. `POST /api/perfil/crm/solicitacao`, sem corpo, usa exclusivamente o profissional autenticado e os dados salvos no banco. Passa a `EM_ANALISE`. Repetir um pedido em análise ou já conferido não cria outra solicitação. Limite: 5 chamadas em 15 minutos por conta e instância.
4. No painel administrativo, a equipe abre a consulta oficial e compara nome completo, número, UF e situação ativa. Marca a caixa de conferência e confirma, ou registra **Não confirmado** quando há divergência. Indisponibilidade do portal deve manter o pedido em análise.
5. O servidor registra estado, data e ID do último revisor. O selo **CRM conferido** informa que houve conferência manual na data apresentada. Não comprova identidade do usuário nem título de especialista, e não garante situação profissional em tempo real.
6. Alterar nome ou número/UF/sufixo invalida a conferência e o pedido anterior, inclusive se o usuário voltar ao valor antigo. Editar bio, telefone ou endereço preserva a conferência. Nome e registro consultados são comparados em toda resposta como proteção adicional.

O status aparece entre formação e localização no card público, e acima de Registro no detalhe (que continua exigindo login). A área de edição também mostra o status e permite solicitar. CRP, CRN, CREFITO, outros conselhos e números sem conselho identificado aparecem como **Registro não verificado**, sem selo CRM ou botão de consulta médica. Psicólogos usam CRP; não é correto inferir um CRM a partir da formação.

## Contratos

Campos existentes dos DTOs e `numRegistro` são preservados. `PerfilResponseDTO` e `ProfissionalResponseDTO` ganham o objeto adicional:

```json
{"crmVerificacao":{"status":"PENDENTE","conferidoEm":null,"fonte":"https://portal.cfm.org.br/busca-medicos/"}}
```

Estados: `PENDENTE`, `EM_ANALISE`, `VERIFICADO`, `NAO_CONFIRMADO`, `NAO_APLICAVEL`. A data pública só aparece quando o estado é VERIFICADO e há revisor registrado. Pendência/negativa não significam falsificação e não bloqueiam a conta.

| Rota | Acesso e comportamento |
|---|---|
| POST `/validacoes/crm` | Compatibilidade com clientes antigos. Retorna 200 com `valido:null`, `cadastroConsultado:false`, `status:VALIDACAO_MANUAL`. Formato não confirma existência. Entrada inválida: 400; limite existente de 20 chamadas/15 min por IP/instância. |
| POST `/api/perfil/crm/solicitacao` | Só profissional autenticado, sem ID recebido do cliente. Retorna estado da conferência. 400 para conselho/formato incompatível; 429 para excesso. |
| GET `/api/admin/crm/solicitacoes?pagina=0` | Só ADMIN; Page com 25 pedidos por página. Campos: id, nome, numRegistro, solicitadoEm, revisao. |
| PUT `/api/admin/crm/solicitacoes/{id}` | Só ADMIN. Corpo: `{"revisao":1,"confirmado":true}`. Usa dados persistidos, bloqueio pessimista e revisão do pedido. 409 se pedido mudou/já foi analisado. |

A revisão e a edição de perfil recarregam a entidade sob bloqueio para não usar o estado antigo carregado pelo filtro JWT. Campos internos não são aceitos pelos DTOs de cadastro/edição e não são expostos publicamente. Nenhuma aprovação depende de localStorage ou de um clique no link externo. O link do portal é fixo, sem parâmetros supostos ou envio automático de dados.

## Aplicação no PostgreSQL e Render

1. Faça backup do banco. Execute `docs/sql/2026-10-26-crm-conferencia-manual.sql` no SQL Editor do Neon **antes de publicar o backend**, principalmente se `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` estiver no Render. O SQL é idempotente: adiciona sete colunas e um índice, sem alterar contas ou aprovar registros existentes. Não precisa mudar ddl-auto para contornar validação de esquema.
2. Execute com Java 21: `./gradlew test` (Windows: `.\gradlew.bat test`), `./gradlew bootJar` e `docker build -t tcc-backend .`.
3. Publique o backend e depois o frontend. Não há nova variável obrigatória. `CFM_CHAVE` pode ser removida se existir, pois não é usada mais.
4. Use uma conta ADMIN existente no fluxo seguro de convites; não crie credenciais públicas. Confira o ciclo completo em homologação com um registro cuja consulta seja autorizada.
5. O Android mantém as rotas/campos atuais e aceita cadastro sem WS. Não há alteração de telas do aplicativo neste pacote. Os DTOs Android precisam ignorar campos extras, como já é habitual com Gson; confirmar no build do app antes de publicar. O status visual novo pertence ao site.

Para desfazer, reverta os PRs e mantenha as colunas adicionadas, sem apagar as conferências. Atenção: reverter apenas o backend restaura o bloqueio da versão anterior quando não houver chave; preferir correção incremental. Nenhuma ação foi feita no banco ou Render nesta entrega.

## Testes e limites

Adicionados/atualizados testes Java para formato, estados, cadastro sem serviço pago, proteção contra autovalidação, proprietário, revisão antiga, idempotência e invalidação permanente por alteração de nome. Usam o H2 e configuração existentes. Não foram executados: somente Java 17 está instalado e o wrapper não consegue baixar Gradle 9.5.1 (`Network is unreachable`). `bootJar`, PostgreSQL, Docker e consulta manual real pendentes.

Frontend: 12 testes Node passaram (7 novos de CRM + 5 existentes de CPF/recuperação), e a sintaxe de todos os JS foi verificada. A suíte Playwright foi atualizada para retirar a consulta paga e há um novo cenário CRM, mas os testes de navegador e a revisão visual nas larguras 1440/1024/768/480/390/360 não executaram: não há binário local e o navegador remoto recusou conexão ao servidor local. Não reutilizar os resultados visuais da entrega anterior como validação deste fluxo.

Melhorias futuras: monitorar tempo de análise, registrar histórico completo das decisões (atualmente mantém só a última), prever reconferência periódica e estruturar conselho/UF de forma coordenada com o aplicativo. Não se adiciona prazo de validade de selo sem regra definida pelo projeto.
