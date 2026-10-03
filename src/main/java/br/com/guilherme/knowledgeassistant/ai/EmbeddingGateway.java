package br.com.guilherme.knowledgeassistant.ai;

import java.util.List;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("knowledge")
public class EmbeddingGateway {
    private final EmbeddingModel model;
    private final String modelKey;
    private final int dimension;

    public EmbeddingGateway(EmbeddingModel model,
                            @Value("${langchain4j.ollama.embedding-model.model-name:nomic-embed-text:v1.5}") String modelKey,
                            @Value("${knowledge-assistant.pgvector.dimension:768}") int dimension) {
        this.model = model;
        this.modelKey = modelKey;
        this.dimension = dimension;
    }

    public String modelKey() {
        return modelKey;
    }

    public List<Embedding> embedAll(List<TextSegment> segments) {
        try {
            List<Embedding> embeddings = model.embedAll(segments).content();
            if (embeddings == null || embeddings.size() != segments.size()) {
                throw new IllegalStateException("Quantidade de vetores incompatível.");
            }
            embeddings.forEach(this::validate);
            return embeddings;
        } catch (RuntimeException exception) {
            throw new KnowledgeException(HttpStatus.BAD_GATEWAY, "Falha ao gerar embeddings.", exception);
        }
    }

    public Embedding embedQuery(String query) {
        return embedAll(List.of(TextSegment.from(query))).getFirst();
    }

    private void validate(Embedding embedding) {
        if (embedding == null || embedding.dimension() != dimension) {
            throw new IllegalStateException("Dimensão do vetor incompatível com a tabela.");
        }
        for (float value : embedding.vector()) {
            if (!Float.isFinite(value)) {
                throw new IllegalStateException("Vetor contém valor não finito.");
            }
        }
    }
}
