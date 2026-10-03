package br.com.guilherme.knowledgeassistant;

import br.com.guilherme.knowledgeassistant.dto.DocumentRequest;
import br.com.guilherme.knowledgeassistant.ingestion.DocumentChunker;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {
    @Test
    void preservesOriginAndDeterministicIds() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var chunker = new DocumentChunker(factory.getValidator(), 160, 30);
            var request = new DocumentRequest("manual", "Manual", "Solicite acesso pelo portal interno. ".repeat(30));
            var first = chunker.split(request, "model-v1");
            var second = chunker.split(request, "model-v1");
            assertThat(first.size()).isGreaterThan(1);
            assertThat(first).allSatisfy(segment -> {
                assertThat(segment.text().length()).isLessThanOrEqualTo(160);
                assertThat(segment.metadata().getString("documentId")).isEqualTo("manual");
            });
            assertThat(first.stream().map(s -> s.metadata().getString("chunkId")).toList())
                .isEqualTo(second.stream().map(s -> s.metadata().getString("chunkId")).toList());
        }
    }
}
