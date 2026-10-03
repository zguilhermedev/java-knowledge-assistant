package br.com.guilherme.knowledgeassistant.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("knowledge")
public class StreamingConfiguration {
    @Bean(destroyMethod = "close")
    ExecutorService streamingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
