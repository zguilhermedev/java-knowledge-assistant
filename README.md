# Knowledge Assistant

Assistente de documentação técnica em Java 25, com LangChain4j, Ollama e PostgreSQL/pgvector.
Importa documentos, recupera trechos por similaridade, responde com fontes verificadas, consulta status por tools e transmite respostas via SSE.

## Funcionalidades

| Operação | Endpoint | Resultado |
| --- | --- | --- |
| Saúde HTTP | `GET /actuator/health` | Disponibilidade básica da aplicação |
| Prévia da divisão | `POST /documents/preview` | Trechos e metadados, sem inferência nem gravação |
| Indexação | `POST /documents` | Recibo `INDEXED` com quantidade de trechos |
| Importação de Markdown | `POST /documents/corpus` | Recibos dos arquivos do corpus configurado |
| Busca semântica | `POST /documents/search` | Trechos, origem, IDs e scores |
| RAG explícito | `POST /questions/rag` | Resposta estruturada com fontes citadas |
| Resposta com tools | `POST /questions` | Documentação, status ou combinação dos dois |
| Streaming de RAG | `POST /questions/stream` | Eventos SSE com fragmentos e resposta final validada |

Os documentos em `docs/corpus` descrevem serviços fictícios. O gateway de status é **simulado**; não consulta serviços externos. A resposta informa essa condição em `serviceStatuses[].simulated`.

## Stack

| Componente | Versão |
| --- | --- |
| Java | 25, sem preview |
| Spring Boot | 4.1.1, Web MVC, Validation e Actuator |
| LangChain4j | BOM/core 1.21.0 |
| Integrações LangChain4j | Ollama Boot 4 e pgvector 1.21.0-beta31 |
| Maven | Wrapper 3.9.16 |
| PostgreSQL + pgvector | `pgvector/pgvector:0.8.7-pg17` |
| Ollama | `ollama/ollama:0.35.1` |

A integração de IA usa LangChain4j. Spring Boot fornece HTTP, configuração, injeção de dependências e validação.

## Build e execução no host

Requer JDK 25. O build padrão usa modelos determinísticos e armazenamento em memória, sem Docker ou inferência real.

No PowerShell, ajuste o caminho do JDK:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Em Linux/macOS, configure `JAVA_HOME` para o JDK 25 e use `./mvnw`.

Para executar apenas o bootstrap HTTP:

```powershell
.\mvnw.cmd spring-boot:run
```

Sem perfis ativos, somente o bootstrap e o Actuator ficam disponíveis, na porta **8082**. O healthcheck não comprova que modelos estão baixados ou que a inferência está funcionando.

Para habilitar todas as funcionalidades, inicie a infraestrutura na raiz do repositório:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose config --quiet
docker compose up -d --wait
docker compose exec ollama ollama pull nomic-embed-text:v1.5
docker compose exec ollama ollama pull llama3.2:3b
docker compose exec ollama ollama list
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=knowledge,ollama,pgvector'
```

Requer Docker com containers Linux e Compose v2 com suporte a `--wait`. O download dos modelos pode levar vários minutos.

## Executar a aplicação em Docker

O Dockerfile usa JDK 25 no build e JRE 25 na imagem final. O processo da aplicação roda com usuário sem privilégios de root. O Compose adicional conecta a aplicação ao banco e ao Ollama por nomes de serviço, com o corpus montado para leitura.

Primeiro prepare a infraestrutura e baixe os modelos como nos comandos acima. Depois:

```powershell
docker compose -f compose.yaml -f compose.app.yaml config --quiet
docker compose -f compose.yaml -f compose.app.yaml up -d --build --wait
curl.exe --fail http://localhost:8082/actuator/health
docker compose -f compose.yaml -f compose.app.yaml logs app
```

Encerre uma aplicação no host antes de usar a mesma porta. Os healthchecks controlam a ordem de inicialização, mas não baixam modelos nem indexam documentos. Após iniciar, importe o corpus pela API.

Para parar o conjunto preservando os volumes de banco e modelos:

```powershell
docker compose -f compose.yaml -f compose.app.yaml down
```

## Usar a API

Em outro terminal, com todos os perfis ativos:

```powershell
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/document-request.json' http://localhost:8082/documents/preview
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/document-request.json' http://localhost:8082/documents
curl.exe --fail -X POST http://localhost:8082/documents/corpus
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/search-request.json' http://localhost:8082/documents/search
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/question-request.json' http://localhost:8082/questions/rag
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/service-status-question-request.json' http://localhost:8082/questions
curl.exe --fail --no-buffer -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/question-request.json' http://localhost:8082/questions/stream
```

Para Linux/macOS, use `curl` no lugar de `curl.exe`. Os endpoints de pergunta recebem `{"question":"..."}`, com texto obrigatório de até 2.000 caracteres. A rota de tools é `/questions`, no plural.

### Documentos e busca

`POST /documents` e `POST /documents/preview` recebem:

```json
{
  "documentId": "manual-ambiente",
  "title": "Manual do ambiente de testes",
  "content": "Solicite acesso ao ambiente de testes pelo portal interno."
}
```

O ID aceita letras minúsculas ASCII, números e hífens, com até 80 caracteres, começando com letra ou número. Título e conteúdo são obrigatórios, com limites de 200 e 100.000 caracteres.

A divisão usa limite de 800 caracteres e sobreposição configurada de 120. O preview usa a chave de modelo `preview`, por isso seus IDs diferem dos persistidos. A importação do corpus não recebe corpo nem caminho do cliente: lê arquivos `.md` regulares do primeiro nível do diretório configurado, em UTF-8, em ordem de caminho.

A busca recebe:

```json
{
  "query": "Como solicito acesso ao ambiente de testes?",
  "maxResults": 3,
  "minScore": 0.6
}
```

`maxResults` aceita 1 a 10 e `minScore` aceita 0 a 1. O score representa similaridade na recuperação, não uma probabilidade de a resposta estar correta.

### Respostas e fontes

As rotas síncronas retornam `answer`, `kind`, `sources` e `serviceStatuses`.

| `kind` | Significado |
| --- | --- |
| `DOCUMENTATION` | Resposta com trechos citados |
| `SERVICE_STATUS` | Resposta apoiada em status consultado por tool |
| `MIXED` | Documentação e status |
| `INSUFFICIENT_CONTEXT` | As fontes não permitem responder |

O RAG explícito busca primeiro e passa os trechos ao modelo. Sem resultados, retorna contexto insuficiente sem chamar o chat. O fluxo de tools permite ao modelo escolher `searchDocumentation` ou `getServiceStatus`, com argumentos validados, limites de chamadas e handlers de erro explícitos.

O verificador exige um contrato válido e IDs de citações presentes nos trechos realmente recuperados. Retorna somente fontes citadas e status efetivamente consultados. Essa verificação estrutural não garante que toda afirmação esteja semanticamente sustentada.

### Streaming SSE

O streaming usa RAG explícito, sem tools. A resposta tem `Content-Type: text/event-stream`.

| Evento | Conteúdo |
| --- | --- |
| `retrieved` | Trechos candidatos usados como contexto |
| `token` | JSON com um fragmento de texto em `text` |
| `answer` | Resposta final estruturada e validada |
| `done` | Marcador `[DONE]` |
| `error` | Mensagem genérica de falha; não é seguida de `done` |

Os fragmentos são provisórios: use `answer` como resultado final. Sem contexto, são enviados apenas `answer` de contexto insuficiente e `done`. O timeout do emitter é de 150 segundos; o limite de texto recebido é de 8.000 caracteres. O cancelamento do modelo depende do suporte do provider.

Como a rota usa POST com corpo, clientes de navegador podem consumir o stream com `fetch` e leitura incremental. `EventSource` nativo não envia esse POST.

### Erros

Requisições inválidas retornam 400. Falhas de modelo ou de validação da resposta retornam 502; indisponibilidade de busca, armazenamento ou leitura do corpus pode retornar 503. Os erros HTTP tratados usam `ProblemDetail`.

Depois de iniciar um stream, uma falha é comunicada pelo evento `error`, pois o status HTTP pode já ter sido enviado.

## Configuração e persistência

| Serviço | Padrão no host |
| --- | --- |
| Aplicação | `http://localhost:8082` |
| Ollama | `http://localhost:11534` |
| PostgreSQL | `localhost:5433`, banco `knowledge_assistant` |
| Tabela vetorial | `knowledge_embeddings_nomic_v15`, dimensão 768 |

`application.yaml` contém a base; `knowledge` habilita os fluxos da API, `ollama` fornece modelos de chat, chat em streaming e embeddings, e `pgvector` fornece o store. O runtime completo utiliza os três perfis. Ativar somente `knowledge` exige fornecer essas dependências por outra configuração.

As variáveis de ambiente estão em [.env.example](.env.example). Spring Boot no host não lê `.env` automaticamente: exporte as variáveis no terminal ou configure a IDE. O Compose utiliza esse arquivo para interpolação e passa as variáveis da aplicação explicitamente.

No container, o banco usa `postgres:5432`, o Ollama usa `ollama:11434` e o corpus fica em `/app/corpus`. No host, o caminho relativo do corpus é resolvido a partir do diretório de execução.

O SQL habilita `vector` na inicialização de um volume novo. A aplicação cria a tabela quando necessário e preserva a existente. Alterar modelo ou dimensão exige tabela compatível e reindexação; dimensão igual não garante vetores compatíveis.

```powershell
docker compose exec postgres psql -U knowledge_assistant -d knowledge_assistant -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
docker compose exec postgres psql -U knowledge_assistant -d knowledge_assistant -c "SELECT count(*) FROM knowledge_embeddings_nomic_v15;"
```

Reenviar um documento substitui os trechos daquele ID e modelo. Todos os novos vetores são gerados antes da remoção, mas remoção e inserção são operações separadas, sem transação de troca. A importação do corpus é sequencial e pode concluir parcialmente.

Credenciais de exemplo destinam-se ao desenvolvimento local.

## Testes e avaliação

O build padrão executa 19 testes, sem serviços externos:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Inclui contratos HTTP, IDs determinísticos, reingestão, busca/RAG, validação de citações, seleção e erros de tools, eventos SSE e um exemplo de RAG com os componentes de alto nível do LangChain4j.

Para verificar o store real em um PostgreSQL isolado e efêmero:

```powershell
docker compose -f src/test/resources/compose.tests.yaml up -d --wait
try {
    .\mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-it verify
    if ($LASTEXITCODE -ne 0) { throw 'Falha no teste de persistência.' }
} finally {
    docker compose -f src/test/resources/compose.tests.yaml down
}
```

Esse perfil usa a porta 15433, valida gravação, recuperação e remoção por metadados e não usa o banco da aplicação. A porta pode ser ajustada com `-Dit.pgvector.port=15433`; host, banco e credenciais correspondem ao Compose de testes.

Com Ollama ativo e os dois modelos baixados:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -Pmodels-it verify
```

O perfil verifica chat e embeddings reais. Os parâmetros `it.ollama.url`, `it.chat.model`, `it.embedding.model` e `it.embedding.dimension` podem ser ajustados com `-D`. Execute os perfis de integração separadamente.

Para registrar recuperação, tipo de resposta e latência sobre o corpus já importado:

```powershell
.\scripts\evaluate.ps1
```

O relatório fica em `target/evaluation/results.json`. O conjunto em `docs/evaluation/queries.json` é uma avaliação pequena e inspecionável; não certifica qualidade geral nem substitui revisão das respostas.

## Organização

- `src/main/java/br/com/guilherme/knowledgeassistant`: controllers, ingestão, recuperação, serviços, tools e configuração.
- `src/main/resources/prompts`: instruções dos AI Services.
- `src/test`: testes, fixtures HTTP e Compose do banco de testes.
- `docs/corpus`: documentação fictícia usada na importação.
- `docs/evaluation` e `scripts`: conjunto de perguntas e avaliação.
- `infra/postgres/init`: habilitação da extensão pgvector.
- [Arquitetura](docs/architecture.md): fluxos, responsabilidades e limites da implementação.
