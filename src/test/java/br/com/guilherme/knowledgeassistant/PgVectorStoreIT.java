package br.com.guilherme.knowledgeassistant;

import java.util.List;
import java.util.UUID;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.junit.jupiter.api.Test;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
import static org.assertj.core.api.Assertions.assertThat;

class PgVectorStoreIT {
    @Test
    void persistsFiltersAndRemovesUsingActualPostgres() {
        String table = "it_" + UUID.randomUUID().toString().replace("-", "");
        var store = PgVectorEmbeddingStore.builder().host("127.0.0.1")
            .port(Integer.parseInt(System.getProperty("it.pgvector.port", "15433")))
            .database("knowledge_test").user("knowledge_test").password("knowledge_test_local")
            .table(table).dimension(3).createTable(true).dropTableFirst(false).useIndex(false).build();
        var vector = Embedding.from(new float[]{1, 0, 0});
        String id = UUID.randomUUID().toString();
        var segment = TextSegment.from("Acesso pelo portal interno.", new Metadata()
            .put("documentId", "manual").put("chunkId", id).put("title", "Manual")
            .put("embeddingModel", "test-model"));
        var query = EmbeddingSearchRequest.builder().queryEmbedding(vector).maxResults(3).minScore(0.6)
            .filter(metadataKey("embeddingModel").isEqualTo("test-model")).build();
        try {
            store.addAll(List.of(id), List.of(vector), List.of(segment));
            var reopened = PgVectorEmbeddingStore.builder().host("127.0.0.1")
                .port(Integer.parseInt(System.getProperty("it.pgvector.port", "15433")))
                .database("knowledge_test").user("knowledge_test").password("knowledge_test_local")
                .table(table).dimension(3).createTable(false).dropTableFirst(false).useIndex(false).build();
            assertThat(reopened.search(query).matches()).hasSize(1);
            assertThat(reopened.search(query).matches().getFirst().embedded().metadata().getString("chunkId"))
                .isEqualTo(id);
            store.removeAll(metadataKey("documentId").isEqualTo("manual")
                .and(metadataKey("embeddingModel").isEqualTo("test-model")));
            assertThat(store.search(query).matches()).isEmpty();
        } finally { store.removeAll(); }
    }
}
