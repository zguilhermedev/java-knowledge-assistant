package br.com.guilherme.knowledgeassistant.controller;

import br.com.guilherme.knowledgeassistant.dto.DocumentRequest;
import br.com.guilherme.knowledgeassistant.dto.DocumentResponse;
import br.com.guilherme.knowledgeassistant.service.DocumentService;
import jakarta.validation.Valid;
import br.com.guilherme.knowledgeassistant.ingestion.CorpusImporter;
import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.ChunkView;
import br.com.guilherme.knowledgeassistant.ingestion.DocumentChunker;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("knowledge")
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService service;
    private final DocumentChunker chunker;
    private final CorpusImporter corpus;

    public DocumentController(DocumentService service, DocumentChunker chunker, CorpusImporter corpus) {
        this.service = service;
        this.chunker = chunker;
        this.corpus = corpus;
    }

    @PostMapping
    public DocumentResponse ingest(@Valid @RequestBody DocumentRequest request) {
        return service.ingest(request);
    }

    @PostMapping("/preview")
    public List<ChunkView> preview(@Valid @RequestBody DocumentRequest request) {
        return chunker.preview(request);
    }

    @PostMapping("/corpus")
    public List<DocumentResponse> ingestCorpus() {
        return corpus.ingestCorpus();
    }
}
