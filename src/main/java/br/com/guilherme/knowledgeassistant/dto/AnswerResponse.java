package br.com.guilherme.knowledgeassistant.dto;

import java.util.List;

public record AnswerResponse(String answer, Kind kind, List<Source> sources, List<ServiceStatus> serviceStatuses) {
    public enum Kind {
        DOCUMENTATION, SERVICE_STATUS, MIXED, INSUFFICIENT_CONTEXT
    }
}
