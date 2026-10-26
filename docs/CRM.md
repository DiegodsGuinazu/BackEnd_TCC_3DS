# Consulta oficial de CRM

## Integração

`POST /validacoes/crm` recebe `{"numero":"1234567","uf":"SP","nome":"Nome completo"}`.

O serviço chama a operação `Consultar` do WS oficial do CFM, via SOAP 1.1/HTTPS, sem dependências novas. Verifica número, UF, situação regular e correspondência do nome informado, normalizando acentos, caixa e espaços. Não devolve nome, chave ou dados do cadastro consultado. Não comprova a identidade de quem preenche o formulário, nem cria selo público de verificação.

Referências oficiais consultadas em 26/10/2026:

- [Manual WS CFM, revisão de agosto de 2026](https://sis.cfm.org.br/listamedicos/arquivos/manualwebservices.pdf), seções 3.3, 4.1 e 4.2: número natural de até **7 dígitos** na consulta, UF e chave; retorno com prefixos/sufixos; código 8101 para não encontrado.
- [Credenciamento e chave do serviço](https://sis.cfm.org.br/listamedicos/informacoes).
- [Consulta pública manual](https://portal.cfm.org.br/busca-medicos/).

Não se usa algoritmo inventado de dígito verificador ou scraping do portal público.

## Configuração e aplicação

1. Obter credenciamento e chave junto ao CFM. Não contratar serviços ou pagar taxas automaticamente.
2. Configurar `CFM_CHAVE` nas variáveis de ambiente do backend (Render). Nunca colocar a chave no frontend, aplicativo, repositório ou logs.
3. Executar com Java 21: `./gradlew test` e `./gradlew bootJar`.
4. Publicar em homologação e testar conexão ao host oficial `ws.cfm.org.br:8080` por HTTPS/TLS 1.2. O cliente mantém validação normal do certificado e limites de conexão/resposta.
5. Fazer uma consulta com dados autorizados e testar também número não localizado, nome divergente e indisponibilidade. Só então disponibilizar o fluxo de CRM do frontend.

O servidor inicia sem chave; a consulta retorna 503 com mensagem clara. **Novos cadastros com registro explicitamente iniciado por CRM, ou alterações de nome/CRM nesses perfis, ficam bloqueados enquanto o CFM não puder confirmar o registro.** Alterar apenas bio/telefone de um perfil existente não obriga nova consulta.

## Respostas

| HTTP | Resultado |
|---|---|
| 200 | `valido: true`, `cadastroConsultado: true`, `status: REGULAR` quando CRM/UF/nome/situação correspondem |
| 200 | `valido: false`, consulta realizada e `NAO_ENCONTRADO`, `NAO_REGULAR` ou `NOME_DIVERGENTE` |
| 400 | Entrada inválida: nome, UF ou número de 1 a 7 dígitos diferente de zero |
| 429 | Limite por instância excedido: 20 tentativas em 15 minutos |
| 503 | Chave ausente/inválida, timeout, erro do CFM ou resposta inesperada; não significa CRM falso |

Todas as respostas do endpoint de consulta usam `Cache-Control: no-store`. Não há logs de credenciais/dados. O XML é interpretado com DTD e entidades externas desabilitados. O cliente usa timeout, resposta limitada a 256 KiB e endpoint fixo; não aceita URLs do cliente.

## Compatibilidade

DTOs, tabelas e respostas de artigos/profissionais não mudaram. Não há migração nem nova dependência. Cadastro continua recebendo `numRegistro`; o frontend salva CRM como `CRM-SP 1234567`, opcionalmente `-P` para provisório.

O backend também consulta antes de salvar registros explicitamente identificados como CRM. Erro de consulta não salva o perfil parcialmente nem consome o código de confirmação de e-mail. Uma recusa do CFM no cadastro/edição retorna 422. O código de confirmação por e-mail continua com o contrato anterior.

Números legados sem conselho identificado e registros CRP/CRN/CREFITO etc. mantêm o comportamento atual. Como não há campo de conselho/especialidade, não é possível atribuir automaticamente todo número a um CRM ou impedir que um cliente envie um número sem conselho. Não apresentar todos os perfis como verificados. Recomenda-se uma evolução coordenada com o aplicativo para conselho/UF/especialidade estruturados.

Não há alterações no aplicativo neste PR: seus DTOs continuam válidos. Clientes que enviarem `numRegistro` começando por CRM passam pela confirmação oficial; precisam tratar 400/422/503 como os demais erros da API. O seletor e a consulta pontual novos pertencem ao frontend web desta solicitação.

## Testes e limites da validação

Adicionados `CrmServiceTest` e `CrmIntegracaoTest`: formato de sete dígitos/UF, falta de chave, envelopes regulares/provisórios/EMFE, nome divergente, situação irregular, não encontrado, erros de chave/serviço, XML incompleto, bloqueio de XXE, outros conselhos, perfil sem gravação parcial e limite de consultas.

`./gradlew test` foi tentado, mas não executou: distribuição Gradle 9.5.1 indisponível pela rede do ambiente e somente Java 17 instalado. `bootJar`, PostgreSQL e Docker também não foram executados. O CFM real não foi consultado por falta de chave; não há afirmação de validação em produção. Os testes do navegador do frontend passaram com respostas de CRM simuladas.

Para desfazer, reverter o commit deste PR e retirar `CFM_CHAVE` do ambiente se não for mais usada. Não existe mudança de esquema a desfazer.

## Arquivos modificados/adicionados

- `src/main/java/br/com/neurohelp/tcc_backend/Service/CrmService.java`
- `src/main/java/br/com/neurohelp/tcc_backend/Controller/Settings/CrmController.java`
- `src/main/java/br/com/neurohelp/tcc_backend/Controller/Settings/CadastroController.java`
- `src/main/java/br/com/neurohelp/tcc_backend/Controller/Settings/CadastroExceptionHandler.java`
- `src/main/java/br/com/neurohelp/tcc_backend/Controller/Perfil/PerfilController.java`
- `src/main/java/br/com/neurohelp/tcc_backend/Controller/IntegracaoExceptionHandler.java`
- `src/main/java/br/com/neurohelp/tcc_backend/config/SecurityConfig.java`
- `src/main/resources/application.properties`
- `src/test/java/br/com/neurohelp/tcc_backend/Service/CrmServiceTest.java`
- `src/test/java/br/com/neurohelp/tcc_backend/Controller/CrmIntegracaoTest.java`
- `docs/CRM.md`
