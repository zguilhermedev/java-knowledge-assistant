package br.com.guilherme.knowledgeassistant;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestModels {
    private static String firstId(ChatRequest request) {
        var matcher = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
            .matcher(request.messages().toString());
        return matcher.find() ? matcher.group() : null;
    }

    @Bean
    InMemoryEmbeddingStore<TextSegment> testStore() { return new InMemoryEmbeddingStore<>(); }

    @Bean
    EmbeddingModel testEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
                return Response.from(segments.stream().map(segment -> {
                    String text = segment.text().toLowerCase(Locale.ROOT);
                    float[] vector = text.contains("acesso") || text.contains("portal") ? new float[]{1, 0, 0}
                        : text.contains("pagamento") || text.contains("retry") ? new float[]{0, 1, 0}
                        : new float[]{0, 0, 1};
                    return Embedding.from(vector);
                }).toList());
            }
        };
    }

    @Bean
    ChatModel testChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                String question = request.messages().stream().filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast).map(UserMessage::singleText).findFirst().orElse("");
                boolean hasTools = request.toolSpecifications() != null && !request.toolSpecifications().isEmpty();
                if (hasTools && !(request.messages().getLast() instanceof ToolExecutionResultMessage)) {
                    boolean status = question.toLowerCase(Locale.ROOT).contains("status");
                    var tool = ToolExecutionRequest.builder().id("call-1")
                        .name(status ? "getServiceStatus" : "searchDocumentation")
                        .arguments(status ? "{\"serviceName\":\"payment-service\"}"
                            : "{\"query\":\"Como solicito acesso ao ambiente de testes?\"}").build();
                    return ChatResponse.builder().aiMessage(AiMessage.from(tool)).build();
                }
                String id = firstId(request);
                boolean status = hasTools && question.toLowerCase(Locale.ROOT).contains("status");
                String output = status ? "{\"answer\":\"Status simulado: DEGRADED.\",\"contextSufficient\":true,\"citationIds\":[]}"
                    : id != null ? "{\"answer\":\"Solicite o acesso pelo portal interno.\",\"contextSufficient\":true,\"citationIds\":[\"" + id + "\"]}"
                    : "{\"answer\":\"Sem informação.\",\"contextSufficient\":false,\"citationIds\":[]}";
                return ChatResponse.builder().aiMessage(AiMessage.from(output)).build();
            }
        };
    }

    @Bean
    StreamingChatModel testStreamingModel() {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                if (request.messages().toString().contains("falha simulada")) {
                    handler.onError(new IllegalStateException("Falha simulada de provedor"));
                    return;
                }
                String id = firstId(request);
                String answer = "Solicite acesso pelo portal interno. [" + id + "]";
                var handle = new StreamingHandle() {
                    private boolean cancelled;
                    public void cancel() { cancelled = true; }
                    public boolean isCancelled() { return cancelled; }
                };
                handler.onPartialResponse(new PartialResponse(answer), new PartialResponseContext(handle));
                if (!handle.isCancelled()) {
                    handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(answer)).build());
                }
            }
        };
    }
}
