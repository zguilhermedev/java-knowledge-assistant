package br.com.guilherme.knowledgeassistant.dto;

public record ChunkView(String documentId, String chunkId, int index, String title, String text) {
}
