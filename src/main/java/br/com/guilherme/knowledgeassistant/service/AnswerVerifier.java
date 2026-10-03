package br.com.guilherme.knowledgeassistant.service;

import java.util.LinkedHashMap;
import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.AnswerResponse;
import br.com.guilherme.knowledgeassistant.dto.GeneratedAnswer;
import br.com.guilherme.knowledgeassistant.dto.ServiceStatus;
import br.com.guilherme.knowledgeassistant.dto.Source;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("knowledge")
public class AnswerVerifier {
    private final Validator validator;

    public AnswerVerifier(Validator validator) {
        this.validator = validator;
    }

    public AnswerResponse insufficient() {
        return new AnswerResponse("Não há informação suficiente nas fontes consultadas para responder.",
            AnswerResponse.Kind.INSUFFICIENT_CONTEXT, List.of(), List.of());
    }

    public AnswerResponse verify(GeneratedAnswer generated, List<Source> candidates, List<ServiceStatus> statuses) {
        if (generated == null || !validator.validate(generated).isEmpty()) {
            throw invalid("O modelo retornou uma resposta fora do contrato.");
        }
        var available = new LinkedHashMap<String, Source>();
        candidates.forEach(source -> available.put(source.chunkId(), source));
        var ids = generated.citationIds().stream().distinct().toList();
        if (ids.stream().anyMatch(id -> !available.containsKey(id))) {
            throw invalid("O modelo citou uma fonte que não foi recuperada.");
        }
        if (!generated.contextSufficient()) {
            return insufficient();
        }
        List<Source> cited = ids.stream().map(available::get).toList();
        if (cited.isEmpty() && statuses.isEmpty()) {
            throw invalid("O modelo respondeu sem citar documentação ou consultar status.");
        }
        var kind = cited.isEmpty() ? AnswerResponse.Kind.SERVICE_STATUS
            : statuses.isEmpty() ? AnswerResponse.Kind.DOCUMENTATION : AnswerResponse.Kind.MIXED;
        return new AnswerResponse(generated.answer(), kind, cited, List.copyOf(statuses));
    }

    private KnowledgeException invalid(String detail) {
        return new KnowledgeException(HttpStatus.BAD_GATEWAY, detail);
    }
}
