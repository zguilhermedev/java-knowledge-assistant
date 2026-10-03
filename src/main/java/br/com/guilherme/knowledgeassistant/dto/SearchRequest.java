package br.com.guilherme.knowledgeassistant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SearchRequest(
    @NotBlank @Size(max = 2000) String query,
    @NotNull @Min(1) @Max(10) Integer maxResults,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double minScore) {
}
