# Autenticação e acesso ao ambiente de testes

Documentação fictícia para desenvolvimento e avaliação de recuperação.
Identificador do documento: authentication. Serviço: auth-service.

## Acesso ao ambiente

O acesso ao ambiente de testes deve ser solicitado pelo portal interno.
No pedido, informe o nome da equipe e o serviço que será integrado.

## Autenticação

As APIs usam OAuth 2.0 com client credentials para integrações entre serviços.
Solicite um token em POST /oauth/token e envie Authorization: Bearer <token>.
O token expira em 15 minutos. Solicite outro token ao expirar.

## Códigos de erro

HTTP 401 indica token ausente, inválido ou expirado. HTTP 403 indica que a
credencial foi autenticada, mas não tem permissão para executar a operação.
Para pagamento, a credencial precisa do escopo payments:write.
