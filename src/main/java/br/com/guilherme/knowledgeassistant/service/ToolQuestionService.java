package br.com.guilherme.knowledgeassistant.service;

import br.com.guilherme.knowledgeassistant.ai.ToolAssistant;
import br.com.guilherme.knowledgeassistant.dto.AnswerResponse;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import br.com.guilherme.knowledgeassistant.retrieval.RetrievalService;
import br.com.guilherme.knowledgeassistant.tool.KnowledgeTools;
import br.com.guilherme.knowledgeassistant.tool.ServiceStatusGateway;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@Profile("knowledge")
public class ToolQuestionService {
    private final ChatModel model;
    private final RetrievalService retrieval;
    private final ServiceStatusGateway statuses;
    private final AnswerVerifier verifier;
    private final int maxResults;
    private final double minScore;
    private final ObjectMapper mapper;

    public ToolQuestionService(ChatModel model, RetrievalService retrieval, ServiceStatusGateway statuses,
                               AnswerVerifier verifier, @Value("${knowledge-assistant.retrieval.max-results:3}") int maxResults,
                               @Value("${knowledge-assistant.retrieval.min-score:0.6}") double minScore, ObjectMapper mapper) {
        this.model = model;
        this.retrieval = retrieval;
        this.statuses = statuses;
        this.verifier = verifier;
        this.maxResults = maxResults;
        this.minScore = minScore;
        this.mapper = mapper;
    }

    public AnswerResponse answer(String question) {
        var tools = new KnowledgeTools(retrieval, statuses, maxResults, minScore, mapper);
        var assistant = AiServices.builder(ToolAssistant.class).chatModel(model)
            .tools(tools)
            .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.sendExceptionMessageToLlm())
            .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocationUnlessVisibleToLlm())
            .maxToolCallingRoundTrips(4).build();
        try {
            var generated = assistant.answer(question);
            return verifier.verify(generated, tools.retrieved(), tools.statuses());
        } catch (KnowledgeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeException(HttpStatus.BAD_GATEWAY, "Falha ao responder usando ferramentas.", exception);
        }
    }
}
