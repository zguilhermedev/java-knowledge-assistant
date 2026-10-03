package br.com.guilherme.knowledgeassistant.service;

import java.util.List;
import br.com.guilherme.knowledgeassistant.ai.EmbeddingGateway;
import br.com.guilherme.knowledgeassistant.dto.DocumentRequest;
import br.com.guilherme.knowledgeassistant.dto.DocumentResponse;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import br.com.guilherme.knowledgeassistant.ingestion.DocumentChunker;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
@Profile("knowledge")
public class DocumentService {
    private final DocumentChunker chunker;
    private final EmbeddingGateway embeddings;
    private final EmbeddingStore<TextSegment> store;

    public DocumentService(DocumentChunker chunker, EmbeddingGateway embeddings, EmbeddingStore<TextSegment> store) {
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.store = store;
    }

    public synchronized DocumentResponse ingest(DocumentRequest request) {
        List<TextSegment> segments = chunker.split(request, embeddings.modelKey());
        var vectors = embeddings.embedAll(segments);
        List<String> ids = segments.stream().map(s -> s.metadata().getString("chunkId")).toList();
        try {
            store.removeAll(metadataKey("documentId").isEqualTo(request.documentId())
                .and(metadataKey("embeddingModel").isEqualTo(embeddings.modelKey())));
            store.addAll(ids, vectors, segments);
        } catch (RuntimeException exception) {
            throw new KnowledgeException(HttpStatus.SERVICE_UNAVAILABLE, "Falha ao persistir documento.", exception);
        }
        return new DocumentResponse(request.documentId(), request.title(), segments.size(), "INDEXED");
    }
}
