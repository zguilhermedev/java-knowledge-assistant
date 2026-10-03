package br.com.guilherme.knowledgeassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
    @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,79}") String documentId,
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 100_000) String content) {
}
