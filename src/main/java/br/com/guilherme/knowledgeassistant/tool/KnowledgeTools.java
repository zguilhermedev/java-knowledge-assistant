package br.com.guilherme.knowledgeassistant.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.SearchRequest;
import br.com.guilherme.knowledgeassistant.dto.ServiceStatus;
import br.com.guilherme.knowledgeassistant.dto.Source;
import br.com.guilherme.knowledgeassistant.retrieval.RetrievalService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.exception.ToolErrorVisibleToLlm;
import tools.jackson.databind.ObjectMapper;

public class KnowledgeTools {
    private final RetrievalService retrieval;
    private final ServiceStatusGateway statusGateway;
    private final int maxResults;
    private final double minScore;
    private final ObjectMapper mapper;
    private final LinkedHashMap<String, Source> retrieved = new LinkedHashMap<>();
    private final List<ServiceStatus> statuses = new ArrayList<>();
    private int calls;

    public KnowledgeTools(RetrievalService retrieval, ServiceStatusGateway statusGateway, int maxResults, double minScore,
                          ObjectMapper mapper) {
        this.retrieval = retrieval;
        this.statusGateway = statusGateway;
        this.maxResults = maxResults;
        this.minScore = minScore;
        this.mapper = mapper;
    }

    @Tool("Busca regras e procedimentos na documentação técnica. Não consulta disponibilidade atual de serviços. "
        + "O resultado contém trechos e chunkId que podem ser citados.")
    public List<Source> searchDocumentation(@P("query obrigatório: pergunta não vazia sobre documentação, "
                                               + "máximo 2000 caracteres") String query) {
        checkBudget();
        if (query == null || query.isBlank() || query.length() > 2000) {
            throw ToolErrorVisibleToLlm.from("searchDocumentation requer o argumento query não vazio, "
                + "com no máximo 2000 caracteres. Para status atual, use getServiceStatus com serviceName.");
        }
        var results = retrieval.search(new SearchRequest(query.strip(), maxResults, minScore));
        results.forEach(source -> retrieved.put(source.chunkId(), source));
        return results;
    }

    @Tool("Consulta status atual simulado de um serviço. Use para disponibilidade atual, não para regras da API.")
    public String getServiceStatus(@P("serviceName obrigatório: nome exato do serviço, por exemplo payment-service; "
                                      + "máximo 80 caracteres") String serviceName) {
        checkBudget();
        if (serviceName == null || serviceName.isBlank() || serviceName.length() > 80) {
            throw ToolErrorVisibleToLlm.from("getServiceStatus requer o argumento serviceName não vazio, "
                + "com no máximo 80 caracteres. Exemplo: payment-service.");
        }
        var status = statusGateway.getServiceStatus(serviceName.strip());
        statuses.add(status);
        return mapper.writeValueAsString(status);
    }

    public List<Source> retrieved() {
        return List.copyOf(retrieved.values());
    }

    public List<ServiceStatus> statuses() {
        return List.copyOf(statuses);
    }

    private void checkBudget() {
        if (++calls > 8) {
            throw new IllegalStateException("Limite de chamadas de ferramentas excedido.");
        }
    }
}
