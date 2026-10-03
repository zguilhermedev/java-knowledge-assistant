package br.com.guilherme.knowledgeassistant;

import java.time.Duration;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RealModelsIT {
    @Test
    void callsActualChatAndEmbeddingModels() {
        String url = System.getProperty("it.ollama.url", "http://localhost:11534");
        String embeddingName = System.getProperty("it.embedding.model", "nomic-embed-text:v1.5");
        String chatName = System.getProperty("it.chat.model", "llama3.2:3b");
        var embeddings = OllamaEmbeddingModel.builder().baseUrl(url).modelName(embeddingName)
            .timeout(Duration.ofSeconds(120)).build();
        var chat = OllamaChatModel.builder().baseUrl(url).modelName(chatName)
            .timeout(Duration.ofSeconds(120)).build();
        assertThat(embeddings.embed("Como solicito acesso ao ambiente de testes?").content().dimension())
            .isEqualTo(Integer.parseInt(System.getProperty("it.embedding.dimension", "768")));
        assertThat(chat.chat("Responda em português: o que é documentação técnica?")).isNotBlank();
    }
}
