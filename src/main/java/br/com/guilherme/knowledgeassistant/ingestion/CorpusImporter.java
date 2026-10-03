package br.com.guilherme.knowledgeassistant.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.DocumentRequest;
import br.com.guilherme.knowledgeassistant.dto.DocumentResponse;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import br.com.guilherme.knowledgeassistant.service.DocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("knowledge")
public class CorpusImporter {
    private final DocumentService documents;
    private final Path directory;

    public CorpusImporter(DocumentService documents,
                          @Value("${knowledge-assistant.corpus-path:docs/corpus}") String directory) {
        this.documents = documents;
        this.directory = Path.of(directory);
    }

    public List<DocumentResponse> ingestCorpus() {
        try (var files = Files.list(directory)) {
            var paths = files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".md")).sorted().toList();
            List<DocumentResponse> result = new ArrayList<>();
            for (Path path : paths) {
                if (Files.size(path) > 400_000) {
                    throw new KnowledgeException(HttpStatus.BAD_REQUEST, "Arquivo do corpus excede o limite.");
                }
                String content = Files.readString(path, StandardCharsets.UTF_8);
                String id = path.getFileName().toString().replaceFirst("\\.md$", "");
                String title = content.lines().filter(line -> line.startsWith("# "))
                    .findFirst().map(line -> line.substring(2).strip()).orElse(id);
                result.add(documents.ingest(new DocumentRequest(id, title, content)));
            }
            return List.copyOf(result);
        } catch (IOException exception) {
            throw new KnowledgeException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível ler o corpus.", exception);
        }
    }
}
