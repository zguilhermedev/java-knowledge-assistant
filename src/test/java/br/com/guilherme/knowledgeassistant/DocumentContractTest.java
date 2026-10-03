package br.com.guilherme.knowledgeassistant;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "knowledge-assistant.pgvector.dimension=3")
@Import(TestModels.class)
@AutoConfigureMockMvc
@ActiveProfiles("knowledge")
class DocumentContractTest {
    @Autowired MockMvc mvc;

    @Test
    void validatesDocumentWithoutCallingModels() throws Exception {
        mvc.perform(post("/documents").contentType(MediaType.APPLICATION_JSON).content("""
                {"documentId":"manual","title":"Manual","content":"Solicite acesso no portal."}
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("INDEXED"));
    }

    @Test
    void rejectsBlankInput() throws Exception {
        mvc.perform(post("/documents").contentType(MediaType.APPLICATION_JSON).content("""
                {"documentId":"INVALID ID","title":"","content":""}
                """))
            .andExpect(status().isBadRequest());
    }
}
