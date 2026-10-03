package br.com.guilherme.knowledgeassistant.ai;

import br.com.guilherme.knowledgeassistant.dto.GeneratedAnswer;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ToolAssistant {
    @SystemMessage(fromResource = "/prompts/tools-system.txt")
    GeneratedAnswer answer(@UserMessage String question);
}
