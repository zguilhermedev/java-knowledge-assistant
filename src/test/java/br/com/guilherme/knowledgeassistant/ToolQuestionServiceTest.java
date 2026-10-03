package br.com.guilherme.knowledgeassistant;

import java.util.ArrayList;
import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.AnswerResponse;
import br.com.guilherme.knowledgeassistant.exception.KnowledgeException;
import br.com.guilherme.knowledgeassistant.retrieval.RetrievalService;
import br.com.guilherme.knowledgeassistant.service.AnswerVerifier;
import br.com.guilherme.knowledgeassistant.service.ToolQuestionService;
import br.com.guilherme.knowledgeassistant.tool.ServiceStatusGateway;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ToolQuestionServiceTest {
    private static final String STATUS_ANSWER = """
        {"answer":"Status simulado: DEGRADED.","contextSufficient":true,"citationIds":[]}
        """;

    @Test
    void returnsActualStatusWithoutSearchingDocuments() {
        var retrieval = mock(RetrievalService.class);
        var model = new ScriptedModel(List.of(
            call("getServiceStatus", "{\"serviceName\":\"payment-service\"}"), AiMessage.from(STATUS_ANSWER)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = service(model, retrieval, new ServiceStatusGateway(), factory.getValidator());
            var result = service.answer("Qual é o status atual do payment-service?");
            assertThat(result.kind()).isEqualTo(AnswerResponse.Kind.SERVICE_STATUS);
            assertThat(result.sources()).isEmpty();
            assertThat(result.serviceStatuses()).singleElement().satisfies(status -> {
                assertThat(status.serviceName()).isEqualTo("payment-service");
                assertThat(status.status()).isEqualTo("DEGRADED");
                assertThat(status.simulated()).isTrue();
            });
            verifyNoInteractions(retrieval);
        }
    }

    @Test
    void feedsInvalidSearchArgumentBackToModelAndAllowsStatusRetry() {
        var retrieval = mock(RetrievalService.class);
        when(retrieval.search(any())).thenThrow(new KnowledgeException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Parâmetros de busca inválidos."));
        var model = new ScriptedModel(List.of(
            call("searchDocumentation", "{\"query\":\"\"}"),
            call("getServiceStatus", "{\"serviceName\":\"payment-service\"}"), AiMessage.from(STATUS_ANSWER)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = service(model, retrieval, new ServiceStatusGateway(), factory.getValidator());
            var result = service.answer("Qual é o status atual do payment-service?");
            assertThat(result.kind()).isEqualTo(AnswerResponse.Kind.SERVICE_STATUS);
            assertThat(model.requests.get(1).messages().getLast())
                .isInstanceOfSatisfying(ToolExecutionResultMessage.class, message ->
                    assertThat(message.text()).contains("query", "getServiceStatus"));
            verifyNoInteractions(retrieval);
        }
    }

    @Test
    void allowsModelToCorrectMalformedToolArguments() {
        var retrieval = mock(RetrievalService.class);
        var model = new ScriptedModel(List.of(
            call("getServiceStatus", "{broken}"),
            call("getServiceStatus", "{\"serviceName\":\"payment-service\"}"), AiMessage.from(STATUS_ANSWER)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var result = service(model, retrieval, new ServiceStatusGateway(), factory.getValidator())
                .answer("Qual é o status atual do payment-service?");
            assertThat(result.kind()).isEqualTo(AnswerResponse.Kind.SERVICE_STATUS);
            assertThat(model.requests.get(1).messages().getLast())
                .isInstanceOfSatisfying(ToolExecutionResultMessage.class, message ->
                    assertThat(message.text()).isNotBlank());
            verifyNoInteractions(retrieval);
        }
    }

    @Test
    void doesNotForwardBackendExceptionDetailsToModel() {
        var gateway = mock(ServiceStatusGateway.class);
        when(gateway.getServiceStatus("payment-service"))
            .thenThrow(new IllegalStateException("Detalhe interno do adaptador de status"));
        var model = new ScriptedModel(List.of(
            call("getServiceStatus", "{\"serviceName\":\"payment-service\"}"), AiMessage.from(STATUS_ANSWER)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service = service(model, mock(RetrievalService.class), gateway, factory.getValidator());
            assertThatThrownBy(() -> service.answer("Qual é o status atual do payment-service?"))
                .isInstanceOf(KnowledgeException.class)
                .hasMessage("Falha ao responder usando ferramentas.");
            assertThat(model.requests).hasSize(1);
        }
    }

    private static ToolQuestionService service(ChatModel model, RetrievalService retrieval,
                                               ServiceStatusGateway gateway, jakarta.validation.Validator validator) {
        return new ToolQuestionService(model, retrieval, gateway, new AnswerVerifier(validator),
            3, 0.6, JsonMapper.builder().build());
    }

    private static AiMessage call(String name, String arguments) {
        return AiMessage.from(ToolExecutionRequest.builder().id("call-" + name)
            .name(name).arguments(arguments).build());
    }

    private static class ScriptedModel implements ChatModel {
        private final List<AiMessage> replies;
        private final List<ChatRequest> requests = new ArrayList<>();

        private ScriptedModel(List<AiMessage> replies) {
            this.replies = replies;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            requests.add(request);
            return ChatResponse.builder().aiMessage(replies.get(requests.size() - 1)).build();
        }
    }
}
