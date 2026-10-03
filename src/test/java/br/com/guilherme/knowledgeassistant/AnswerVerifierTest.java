package br.com.guilherme.knowledgeassistant;

import java.time.Instant;
import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.AnswerResponse;
import br.com.guilherme.knowledgeassistant.dto.GeneratedAnswer;
import br.com.guilherme.knowledgeassistant.dto.ServiceStatus;
import br.com.guilherme.knowledgeassistant.dto.Source;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import br.com.guilherme.knowledgeassistant.service.AnswerVerifier;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerVerifierTest {
    @Test
    void keepsOnlyCitedSourcesAndRejectsInventedIds() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var verifier = new AnswerVerifier(factory.getValidator());
            var first = new Source("a", "a-1", "A", "Portal interno", 0.9);
            var second = new Source("b", "b-1", "B", "Outro trecho", 0.8);
            var result = verifier.verify(new GeneratedAnswer("Use o portal.", true, List.of("a-1")),
                List.of(first, second), List.of());
            assertThat(result.sources()).containsExactly(first);
            assertThatThrownBy(() -> verifier.verify(new GeneratedAnswer("Inventada", true, List.of("x")),
                List.of(first), List.of())).isInstanceOf(KnowledgeException.class);
        }
    }

    @Test
    void rejectsInvalidOutputAndUsesApplicationFallback() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var verifier = new AnswerVerifier(factory.getValidator());
            assertThatThrownBy(() -> verifier.verify(new GeneratedAnswer("", true, List.of()), List.of(), List.of()))
                .isInstanceOf(KnowledgeException.class);
            assertThatThrownBy(() -> verifier.verify(new GeneratedAnswer("Sem fonte", true, List.of()), List.of(), List.of()))
                .isInstanceOf(KnowledgeException.class);
            var result = verifier.verify(new GeneratedAnswer("Texto a descartar", false, List.of()), List.of(), List.of());
            assertThat(result.kind()).isEqualTo(AnswerResponse.Kind.INSUFFICIENT_CONTEXT);
            assertThat(result.answer()).isEqualTo(verifier.insufficient().answer());
        }
    }

    @Test
    void acceptsActualToolResultWithoutDocumentReferences() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var verifier = new AnswerVerifier(factory.getValidator());
            var status = new ServiceStatus("payment-service", "DEGRADED", true, Instant.EPOCH);
            var result = verifier.verify(new GeneratedAnswer("Status simulado: DEGRADED.", true, List.of()),
                List.of(), List.of(status));
            assertThat(result.kind()).isEqualTo(AnswerResponse.Kind.SERVICE_STATUS);
            assertThat(result.serviceStatuses()).containsExactly(status);
        }
    }
}
