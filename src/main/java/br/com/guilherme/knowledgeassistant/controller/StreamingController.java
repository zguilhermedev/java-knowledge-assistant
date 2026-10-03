package br.com.guilherme.knowledgeassistant.controller;

import br.com.guilherme.knowledgeassistant.dto.QuestionRequest;
import br.com.guilherme.knowledgeassistant.service.QuestionStreamingService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Profile("knowledge")
public class StreamingController {
    private final QuestionStreamingService streaming;

    public StreamingController(QuestionStreamingService streaming) {
        this.streaming = streaming;
    }

    @PostMapping(value = "/questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody QuestionRequest request) {
        return streaming.stream(request.question());
    }
}
