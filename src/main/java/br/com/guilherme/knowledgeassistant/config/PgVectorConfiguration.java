package br.com.guilherme.knowledgeassistant.config;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("pgvector")
@EnableConfigurationProperties(PgVectorProperties.class)
public class PgVectorConfiguration {
    @Bean
    PgVectorEmbeddingStore embeddingStore(PgVectorProperties properties) {
        return PgVectorEmbeddingStore.builder()
            .host(properties.host()).port(properties.port())
            .database(properties.database()).user(properties.user()).password(properties.password())
            .table(properties.table()).dimension(properties.dimension())
            .createTable(true).dropTableFirst(false).useIndex(false).build();
    }
}
