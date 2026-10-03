package br.com.guilherme.knowledgeassistant.controller;

import br.com.guilherme.knowledgeassistant.dto.AnswerResponse;
import br.com.guilherme.knowledgeassistant.dto.QuestionRequest;
import br.com.guilherme.knowledgeassistant.service.RagQuestionService;
import br.com.guilherme.knowledgeassistant.service.ToolQuestionService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("knowledge")
public class QuestionController {
    private final RagQuestionService rag;
    private final ToolQuestionService tools;

    public QuestionController(RagQuestionService rag, ToolQuestionService tools) {
        this.rag = rag;
        this.tools = tools;
    }

    @PostMapping("/questions")
    public AnswerResponse answer(@Valid @RequestBody QuestionRequest request) {
        return tools.answer(request.question());
    }

    @PostMapping("/questions/rag")
    public AnswerResponse rag(@Valid @RequestBody QuestionRequest request) {
        return rag.answer(request.question());
    }
}
