package br.com.guilherme.knowledgeassistant.retrieval;

import java.util.List;
import br.com.guilherme.knowledgeassistant.ai.EmbeddingGateway;
import br.com.guilherme.knowledgeassistant.dto.SearchRequest;
import br.com.guilherme.knowledgeassistant.dto.Source;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@Profile("knowledge")
public class RetrievalService {
    private final EmbeddingGateway embeddings;
    private final EmbeddingStore<TextSegment> store;
    private final Validator validator;

    public RetrievalService(EmbeddingGateway embeddings, EmbeddingStore<TextSegment> store, Validator validator) {
        this.embeddings = embeddings;
        this.store = store;
        this.validator = validator;
    }

    public List<Source> search(SearchRequest request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new KnowledgeException(HttpStatus.BAD_REQUEST, "Parâmetros de busca inválidos.");
        }
        var vector = embeddings.embedQuery(request.query());
        try {
            return store.search(EmbeddingSearchRequest.builder().queryEmbedding(vector)
                    .maxResults(request.maxResults()).minScore(request.minScore())
                    .filter(metadataKey("embeddingModel").isEqualTo(embeddings.modelKey())).build())
                .matches().stream().map(match -> {
                    var segment = match.embedded();
                    return new Source(segment.metadata().getString("documentId"),
                        segment.metadata().getString("chunkId"), segment.metadata().getString("title"),
                        segment.text(), match.score());
                }).toList();
        } catch (RuntimeException exception) {
            throw new KnowledgeException(HttpStatus.SERVICE_UNAVAILABLE, "Falha na busca vetorial.", exception);
        }
    }
}
