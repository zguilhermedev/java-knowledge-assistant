package br.com.guilherme.knowledgeassistant;

import java.util.concurrent.atomic.AtomicReference;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LibraryRagExampleTest {
    interface Assistant {
        @SystemMessage("Use somente o contexto recuperado para responder em português.")
        String answer(String question);
    }

    @Test
    void comparesLibraryPipelineWithOurExplicitPipeline() {
        var test = new TestModels();
        var store = new InMemoryEmbeddingStore<TextSegment>();
        var model = test.testEmbeddingModel();
        var ingestor = EmbeddingStoreIngestor.builder().documentSplitter(DocumentSplitters.recursive(800, 120))
            .embeddingModel(model).embeddingStore(store).build();
        ingestor.ingest(Document.from("Solicite acesso pelo portal interno.",
            new Metadata().put("documentId", "manual").put("title", "Manual")));
        var retriever = EmbeddingStoreContentRetriever.builder().embeddingStore(store).embeddingModel(model)
            .maxResults(3).minScore(0.6).build();
        assertThat(retriever.retrieve(Query.from("Como solicito acesso?"))).hasSize(1);
        var captured = new AtomicReference<ChatRequest>();
        var chat = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                captured.set(request);
                return ChatResponse.builder().aiMessage(AiMessage.from("Solicite acesso pelo portal interno.")).build();
            }
        };
        var assistant = AiServices.builder(Assistant.class).chatModel(chat)
            .contentRetriever(retriever).build();
        assertThat(assistant.answer("Como solicito acesso?")).isNotBlank();
        assertThat(captured.get().messages().toString()).contains("Solicite acesso pelo portal interno.");
    }
}
