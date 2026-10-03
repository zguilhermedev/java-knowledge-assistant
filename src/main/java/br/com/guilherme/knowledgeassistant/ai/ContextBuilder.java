package br.com.guilherme.knowledgeassistant.ai;

import java.util.List;
import br.com.guilherme.knowledgeassistant.dto.Source;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("knowledge")
public class ContextBuilder {
    private final ObjectMapper mapper;

    public ContextBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String render(List<Source> sources) {
        return mapper.writeValueAsString(sources);
    }
}
