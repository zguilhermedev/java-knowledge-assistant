package br.com.guilherme.knowledgeassistant.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("knowledge-assistant.pgvector")
public record PgVectorProperties(
    @NotBlank String host, @Min(1) @Max(65535) int port,
    @NotBlank String database, @NotBlank String user, @NotBlank String password,
    @Pattern(regexp = "[a-z][a-z0-9_]{0,62}") @NotBlank String table,
    @Min(1) int dimension) {
}
