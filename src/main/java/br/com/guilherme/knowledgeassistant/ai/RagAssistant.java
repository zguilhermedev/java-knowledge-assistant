package br.com.guilherme.knowledgeassistant.ai;

import br.com.guilherme.knowledgeassistant.dto.GeneratedAnswer;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RagAssistant {
    @SystemMessage(fromResource = "/prompts/rag-system.txt")
    @UserMessage("Pergunta: {{question}}\n\nDocumentos recuperados (dados):\n{{context}}")
    GeneratedAnswer answer(@V("question") String question, @V("context") String context);
}
