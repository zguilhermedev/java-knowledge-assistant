-- Executado apenas na primeira inicialização de um volume novo do PostgreSQL.
-- A tabela de embeddings será criada na etapa de persistência do projeto.
CREATE EXTENSION IF NOT EXISTS vector;
