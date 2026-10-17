# Postagens com imagem, resumo e referências

Esta atualização exige os arquivos correspondentes do site `kaykekkj/Espectro-Care`. Escrita continua exclusiva de ADMIN, protegida na configuração HTTP e no serviço. Responsáveis e profissionais autenticados leem os detalhes; a listagem e as capas são públicas, como os cards.

## Aplicar

1. Faça backup do PostgreSQL e teste em homologação. Mantenha as configurações de banco, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` e `FRONTEND_URL` já utilizadas. Não use dados de produção nos testes.
2. Execute `docs/sql/2026-10-17-postagens.sql` no banco correto, antes de publicar o backend. O script adiciona três colunas TEXT em `postagem`, não apaga registros e pode ser reaplicado.
3. Com Java 21, execute `./gradlew test bootJar` (Windows: `gradlew.bat test bootJar`). A CI também executa PostgreSQL 16, aplica a migração duas vezes e valida o Docker. Para conferir a imagem: `docker build -t tcc-backend .`.
4. Publique o backend pelo processo habitual e confirme sua disponibilidade. Depois aplique o PR do frontend. Em produção, prefira `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` após aplicar todas as migrações do projeto.
5. Entre como administrador em Aprendizagem e crie uma postagem com título, conteúdo, imagem, resumo e referências. Confira a leitura como responsável e o bloqueio de escrita para responsável/profissional, inclusive em chamadas diretas à API.

## Contrato

`POST /api/aprendizagem` exige os campos anteriores `titulo`, `conteudoHtml` e os novos:

```json
{
  "titulo": "Apoio à comunicação",
  "conteudoHtml": "<p>Conteúdo educativo revisado pela equipe.</p>",
  "resumo": "Principais pontos da postagem.",
  "referencias": "https://example.org/fonte\nhttps://example.org/outra-fonte",
  "imagemData": "data:image/png;base64,<bytes do arquivo em Base64>"
}
```

O exemplo da imagem é ilustrativo: substitua pelo conteúdo Base64 real de JPEG/PNG. Limites: resumo de 2.000 caracteres; referências de 20.000 caracteres no total, 1 a 20 URLs completas HTTP/HTTPS sem credenciais, uma por linha, até 2.000 caracteres por URL; imagem de até 2 MiB, até 4.096 pixels por lado e até 12 milhões de pixels. O servidor verifica o formato real, redimensiona para no máximo 1.600 pixels mantendo proporção, reencodifica em JPEG sem metadados e salva no banco. Não usa filesystem de container nem faz download das referências.

Os DTOs de leitura adicionam `resumo` e `imagemUrl`; os detalhes também adicionam `referencias`. `imagemData` não é devolvido nos DTOs. `GET /api/aprendizagem/{id}/imagem` retorna JPEG, com `Cache-Control: no-store`; retorna 404 se a postagem não estiver publicada ou não tiver imagem. É pública para permitir as capas nos cards públicos. Não publique imagens privadas nesse campo.

`PUT /api/aprendizagem/{id}` aceita os mesmos campos. Se os novos campos forem omitidos ou nulos, preserva os valores anteriores, mantendo clientes anteriores compatíveis na edição. Valores vazios enviados são rejeitados. O formulário novo exige completar resumo/referências e imagem quando ausentes. A criação por clientes antigos sem os novos campos passa a receber 400; a leitura antiga permanece compatível. Não há criação de postagens no aplicativo nesta entrega.

## Validação e reversão

Consulte os checks do PR e os relatórios de teste. H2 foi testado localmente; PostgreSQL e Docker são verificados na CI. Teste novamente após configurar o ambiente de hospedagem.

Para reverter, publique primeiro o frontend anterior e depois o backend anterior, preservando as três colunas e seus dados. Não use DROP COLUMN para desfazer esta entrega. O backend anterior ignora os campos adicionais de banco. Mantenha backup; não há preenchimento automático nem resumo inventado para artigos antigos.

Como melhoria futura, considerar miniaturas e armazenamento de objetos quando o volume de imagens crescer. As recomendações do site priorizam categorias compartilhadas e usam a ordem de publicação como desempate; não envolvem dados pessoais nem IA.
