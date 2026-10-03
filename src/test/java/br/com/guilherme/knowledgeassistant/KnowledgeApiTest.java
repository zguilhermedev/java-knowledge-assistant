package br.com.guilherme.knowledgeassistant;

import br.com.guilherme.knowledgeassistant.dto.DocumentRequest;
import br.com.guilherme.knowledgeassistant.service.DocumentService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "knowledge-assistant.pgvector.dimension=3")
@AutoConfigureMockMvc
@ActiveProfiles("knowledge")
@Import(TestModels.class)
class KnowledgeApiTest {
    @Autowired MockMvc mvc;
    @Autowired DocumentService documents;
    @Autowired InMemoryEmbeddingStore<TextSegment> store;

    @BeforeEach
    void reset() {
        store.removeAll();
        documents.ingest(new DocumentRequest("manual", "Manual", "O acesso deve ser solicitado pelo portal interno."));
    }

    @Test
    void searchesAndAnswersWithARealRetrievedId() throws Exception {
        mvc.perform(post("/documents/search").contentType(MediaType.APPLICATION_JSON).content("""
                {"query":"Como solicito acesso?","maxResults":3,"minScore":0.6}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].documentId").value("manual"));
        mvc.perform(post("/questions/rag").contentType(MediaType.APPLICATION_JSON).content("""
                {"question":"Como solicito acesso?"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.kind").value("DOCUMENTATION"))
            .andExpect(jsonPath("$.sources[0].documentId").value("manual"));
    }

    @Test
    void handlesOutOfScopeAndRejectsInvalidSearch() throws Exception {
        mvc.perform(post("/questions/rag").contentType(MediaType.APPLICATION_JSON).content("""
                {"question":"Qual é a política de férias?"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.kind").value("INSUFFICIENT_CONTEXT"));
        mvc.perform(post("/documents/search").contentType(MediaType.APPLICATION_JSON).content("""
                {"query":"acesso","maxResults":100,"minScore":2}
                """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void executesStatusToolWithoutDocumentContext() throws Exception {
        store.removeAll();
        mvc.perform(post("/questions").contentType(MediaType.APPLICATION_JSON).content("""
                {"question":"Qual é o status atual do payment-service?"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.kind").value("SERVICE_STATUS"))
            .andExpect(jsonPath("$.serviceStatuses[0].status").value("DEGRADED"))
            .andExpect(jsonPath("$.serviceStatuses[0].simulated").value(true));
    }

    @Test
    void executesDocumentationTool() throws Exception {
        mvc.perform(post("/questions").contentType(MediaType.APPLICATION_JSON).content("""
                {"question":"Como solicito acesso?"}
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sources[0].documentId").value("manual"));
    }

    @Test
    void reingestionRemovesObsoleteChunks() {
        documents.ingest(new DocumentRequest("manual", "Manual", "Acesso pelo portal interno. ".repeat(100)));
        assertThat(store.size()).isGreaterThan(1);
        documents.ingest(new DocumentRequest("manual", "Manual atualizado", "Acesso somente pelo portal interno."));
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void streamsCandidatesThenValidatedAnswerAndCompletion() throws Exception {
        var result = mvc.perform(post("/questions/stream").contentType(MediaType.APPLICATION_JSON).content("""
                {"question":"Como solicito acesso?"}
                """))
            .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5000);
        String body = mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("event:retrieved", "event:token", "event:answer", "event:done");
        assertThat(body).doesNotContain("event:error");
    }

    @Test
    void reportsStreamingErrorWithoutDone() throws Exception {
        var result = mvc.perform(post("/questions/stream").contentType(MediaType.APPLICATION_JSON).content("""
                {"question":"acesso com falha simulada"}
                """))
            .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(5000);
        String body = mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("event:error").doesNotContain("event:done");
    }
}
