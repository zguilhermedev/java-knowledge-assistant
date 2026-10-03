package br.com.guilherme.knowledgeassistant.dto;

public record DocumentResponse(String documentId, String title, int chunkCount, String state) {
}
