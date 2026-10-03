package br.com.guilherme.knowledgeassistant.service;

import java.util.List;
import br.com.guilherme.knowledgeassistant.ai.ContextBuilder;
import br.com.guilherme.knowledgeassistant.ai.RagAssistant;
import br.com.guilherme.knowledgeassistant.dto.AnswerResponse;
import br.com.guilherme.knowledgeassistant.dto.SearchRequest;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import br.com.guilherme.knowledgeassistant.retrieval.RetrievalService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("knowledge")
public class RagQuestionService {
    private final RetrievalService retrieval;
    private final ContextBuilder context;
    private final AnswerVerifier verifier;
    private final RagAssistant assistant;
    private final int maxResults;
    private final double minScore;

    public RagQuestionService(RetrievalService retrieval, ContextBuilder context, AnswerVerifier verifier,
                              ChatModel chat, @Value("${knowledge-assistant.retrieval.max-results:3}") int maxResults,
                              @Value("${knowledge-assistant.retrieval.min-score:0.6}") double minScore) {
        this.retrieval = retrieval;
        this.context = context;
        this.verifier = verifier;
        this.assistant = AiServices.builder(RagAssistant.class).chatModel(chat).build();
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    public AnswerResponse answer(String question) {
        var sources = retrieval.search(new SearchRequest(question, maxResults, minScore));
        if (sources.isEmpty()) {
            return verifier.insufficient();
        }
        try {
            return verifier.verify(assistant.answer(question, context.render(sources)), sources, List.of());
        } catch (KnowledgeException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new KnowledgeException(HttpStatus.BAD_GATEWAY, "Falha ao gerar resposta com contexto.", exception);
        }
    }
}
