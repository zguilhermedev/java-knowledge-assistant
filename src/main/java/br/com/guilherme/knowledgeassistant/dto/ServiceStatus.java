package br.com.guilherme.knowledgeassistant.dto;

import java.time.Instant;

public record ServiceStatus(String serviceName, String status, boolean simulated, Instant observedAt) {
}
