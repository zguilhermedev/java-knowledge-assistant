package br.com.guilherme.knowledgeassistant.dto;

public record Source(String documentId, String chunkId, String title, String excerpt, double score) {
}
