package br.com.guilherme.knowledgeassistant.controller;

import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.SearchRequest;
import br.com.guilherme.knowledgeassistant.dto.Source;
import br.com.guilherme.knowledgeassistant.retrieval.RetrievalService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("knowledge")
public class SearchController {
    private final RetrievalService retrieval;

    public SearchController(RetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    @PostMapping("/documents/search")
    public List<Source> search(@Valid @RequestBody SearchRequest request) {
        return retrieval.search(request);
    }
}
