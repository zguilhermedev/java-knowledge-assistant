package br.com.guilherme.knowledgeassistant;

import java.util.List;
import java.util.Locale;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestModels {
    @Bean
    InMemoryEmbeddingStore<TextSegment> testStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    EmbeddingModel testEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
                return Response.from(segments.stream().map(segment -> {
                    String text = segment.text().toLowerCase(Locale.ROOT);
                    float[] vector = text.contains("acesso") || text.contains("portal")
                        ? new float[]{1, 0, 0}
                        : text.contains("pagamento") || text.contains("retry")
                        ? new float[]{0, 1, 0} : new float[]{0, 0, 1};
                    return Embedding.from(vector);
                }).toList());
            }
        };
    }
}
