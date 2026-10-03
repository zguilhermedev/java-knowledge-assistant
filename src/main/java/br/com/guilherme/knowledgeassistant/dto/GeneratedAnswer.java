package br.com.guilherme.knowledgeassistant.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GeneratedAnswer(
    @NotBlank @Size(max = 8000) String answer,
    @NotNull Boolean contextSufficient,
    @NotNull @Size(max = 10) List<@NotBlank @Size(max = 80) String> citationIds) {
}
