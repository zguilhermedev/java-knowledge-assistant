package br.com.guilherme.knowledgeassistant.ingestion;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.com.guilherme.knowledgeassistant.dto.ChunkView;
import br.com.guilherme.knowledgeassistant.dto.DocumentRequest;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("knowledge")
public class DocumentChunker {
    private final DocumentSplitter splitter;
    private final Validator validator;

    public DocumentChunker(Validator validator,
                           @Value("${knowledge-assistant.ingestion.chunk-size:800}") int size,
                           @Value("${knowledge-assistant.ingestion.overlap:120}") int overlap) {
        if (size < 100 || overlap < 0 || overlap >= size) {
            throw new IllegalArgumentException("Chunk size deve ser >= 100 e 0 <= overlap < size.");
        }
        this.validator = validator;
        this.splitter = DocumentSplitters.recursive(size, overlap);
    }

    public List<TextSegment> split(DocumentRequest request, String modelKey) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new KnowledgeException(HttpStatus.BAD_REQUEST, "Documento inválido.");
        }
        Metadata metadata = new Metadata()
            .put("documentId", request.documentId())
            .put("title", request.title())
            .put("embeddingModel", modelKey);
        List<TextSegment> original = splitter.split(Document.from(request.content().strip(), metadata));
        List<TextSegment> result = new ArrayList<>();
        for (int index = 0; index < original.size(); index++) {
            TextSegment segment = original.get(index);
            String seed = request.documentId() + "\u001f" + modelKey + "\u001f"
                          + index + "\u001f" + segment.text();
            String id = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
            result.add(TextSegment.from(segment.text(), segment.metadata().copy()
                .put("chunkId", id).put("chunkIndex", index)));
        }
        return List.copyOf(result);
    }

    public List<ChunkView> preview(DocumentRequest request) {
        return split(request, "preview").stream().map(segment -> new ChunkView(
            request.documentId(), segment.metadata().getString("chunkId"),
            segment.metadata().getInteger("chunkIndex"), request.title(), segment.text())).toList();
    }
}
