# Knowledge Assistant

Backend Java para ingestão de documentação técnica com LangChain4j e PostgreSQL/pgvector.
A API valida documentos, divide o conteúdo em trechos, gera embeddings e importa um corpus Markdown local.

## Funcionalidades

| Operação | Endpoint | Resultado |
| --- | --- | --- |
| Saúde HTTP | `GET /actuator/health` | Disponibilidade da aplicação |
| Prévia da divisão | `POST /documents/preview` | Trechos e metadados, sem inferência nem gravação |
| Indexação de documento | `POST /documents` | Recibo `INDEXED` e quantidade de trechos |
| Importação do corpus | `POST /documents/corpus` | Recibos dos arquivos `.md` do diretório configurado |

A API de documentos requer os perfis `knowledge,ollama,pgvector`. Busca semântica, respostas RAG, citações, tools e streaming ainda não estão implementados. Os documentos em `docs/corpus` descrevem serviços fictícios.

## Stack

| Componente | Versão |
| --- | --- |
| Java | 25, sem preview |
| Spring Boot | 4.1.1, Web MVC, Validation e Actuator |
| LangChain4j | BOM/core 1.21.0 |
| Integrações LangChain4j | Ollama Boot 4 e pgvector 1.21.0-beta31, gerenciados pelo BOM |
| Maven | Wrapper 3.9.16 |
| PostgreSQL + pgvector | `pgvector/pgvector:0.8.7-pg17` |
| Ollama | `ollama/ollama:0.35.1` |

## Compilar e executar

Requisito: JDK 25. O build padrão executa quatro testes sem Docker ou modelos reais. O wrapper baixa o Maven quando necessário.

No PowerShell, ajuste o caminho do JDK ao seu ambiente:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd --batch-mode --no-transfer-progress verify
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, configure `JAVA_HOME` para o JDK 25:

```bash
./mvnw --batch-mode --no-transfer-progress verify
./mvnw spring-boot:run
```

Sem perfis ativos, a aplicação inicia na porta **8082**, sem modelos ou banco. Remova `SPRING_PROFILES_ACTIVE` se o ambiente já o definir. Em outro terminal:

```powershell
curl.exe --fail http://localhost:8082/actuator/health
```

A resposta HTTP 200 contém `"status":"UP"`. O endpoint não certifica a disponibilidade de Ollama ou PostgreSQL. Encerre com `Ctrl+C` antes de reiniciar.

## Infraestrutura e ingestão

Execute na raiz do repositório. São necessários Docker com containers Linux e Docker Compose v2 com suporte a `--wait`. O Compose inicia somente a infraestrutura; a aplicação roda no host.

Copie o exemplo de ambiente na primeira configuração, preservando um `.env` existente:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose config --quiet
docker compose up -d --wait
docker compose exec ollama ollama pull nomic-embed-text:v1.5
docker compose exec ollama ollama pull llama3.2:3b
docker compose exec ollama ollama list
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=knowledge,ollama,pgvector'
```

O download pode levar vários minutos. A ingestão utiliza o modelo de embeddings. O cliente de chat está configurado separadamente e disponível para o teste de integração. Container saudável não garante modelos baixados.

Em outro terminal:

```powershell
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/document-request.json' http://localhost:8082/documents/preview
curl.exe --fail -H 'Content-Type: application/json' --data-binary '@src/test/resources/fixtures/document-request.json' http://localhost:8082/documents
curl.exe --fail -X POST http://localhost:8082/documents/corpus
```

Os dois primeiros endpoints recebem este contrato:

```json
{
  "documentId": "manual-ambiente",
  "title": "Manual do ambiente de testes",
  "content": "Solicite acesso ao ambiente de testes pelo portal interno."
}
```

O ID aceita letras minúsculas ASCII, números e hífens, com até 80 caracteres; deve começar com letra ou número. Título e conteúdo são obrigatórios, com limites de 200 e 100.000 caracteres. Entrada inválida retorna 400. Falha de embeddings retorna 502; falha de persistência ou leitura do corpus retorna 503. Os erros tratados usam `ProblemDetail`.

Os trechos têm limite padrão de 800 caracteres e sobreposição configurada de 120. O preview usa a chave de modelo `preview`: seus IDs diferem dos persistidos, que incorporam a chave do modelo de embeddings.

`POST /documents/corpus` não recebe corpo nem caminho do cliente. Importa somente arquivos `.md` regulares do primeiro nível do diretório configurado, em ordem de caminho. O nome sem extensão fornece o ID; o primeiro cabeçalho `# ` fornece o título. O conteúdo é lido como UTF-8 e validado pelo mesmo fluxo da ingestão JSON.

## Configuração e persistência

| Serviço | Padrão no host |
| --- | --- |
| Aplicação | `http://localhost:8082` |
| Ollama | `http://localhost:11534` |
| PostgreSQL | `localhost:5433`, banco `knowledge_assistant` |
| Tabela vetorial | `knowledge_embeddings_nomic_v15`, dimensão 768 |

`application.yaml` contém a base. `knowledge` habilita a API e a ingestão; `ollama` configura chat e embeddings; `pgvector` constrói o armazenamento. Ativar somente `knowledge`, sem fornecer modelo e store, impede a inicialização.

As variáveis e seus padrões estão em `.env.example`. Spring Boot no host não lê `.env` automaticamente: exporte as variáveis no terminal ou configure a IDE para alterar os padrões da aplicação. `KNOWLEDGE_CORPUS_PATH` é relativo ao diretório de execução quando não for absoluto.

O SQL habilita `vector` apenas na inicialização de um volume novo. A aplicação cria a tabela quando necessário e preserva a existente. Trocar dimensão ou modelo exige uma tabela compatível e reindexação; dimensão igual não garante compatibilidade dos vetores.

```powershell
docker compose exec postgres psql -U knowledge_assistant -d knowledge_assistant -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
docker compose exec postgres psql -U knowledge_assistant -d knowledge_assistant -c "SELECT count(*) FROM knowledge_embeddings_nomic_v15;"
```

Os comandos usam usuário, banco e tabela padrão. Reenviar um documento substitui os trechos daquele ID e modelo. Os vetores novos são gerados antes da remoção dos antigos, mas remoção e inserção são operações separadas, sem transação para a troca. A importação do corpus é sequencial e pode concluir parcialmente.

As credenciais de exemplo são exclusivas do desenvolvimento local. Para parar preservando os volumes:

```powershell
docker compose down
```

## Testes

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

O build padrão verifica saúde HTTP, validação do contrato, indexação com modelo e store em memória e IDs determinísticos. Não faz inferência real nem comprova persistência PostgreSQL.

Com Ollama ativo e ambos os modelos baixados:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -Pmodels-it verify
```

Esse perfil Maven adiciona `RealModelsIT`, que chama chat e embeddings reais. Os parâmetros `it.ollama.url`, `it.chat.model`, `it.embedding.model` e `it.embedding.dimension` podem ser passados com `-D`; os padrões correspondem ao ambiente acima. O teste verifica dimensão e resposta não vazia, sem avaliar qualidade semântica ou gravação no banco.

## Organização

- `src/main/java/br/com/guilherme/knowledgeassistant`: API, ingestão, adaptação de embeddings e configuração.
- `src/main/resources`: YAML base e configurações por perfil.
- `src/test`: testes e entradas JSON de documentos.
- `docs/corpus`: documentos Markdown usados pela importação.
- `infra/postgres/init`: habilitação da extensão pgvector.
- [Arquitetura](docs/architecture.md): fluxo implementado e limites operacionais.
