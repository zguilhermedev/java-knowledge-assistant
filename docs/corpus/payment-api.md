# API de pagamento

Documentação fictícia para desenvolvimento e avaliação de recuperação.
Identificador do documento: payment-api. Serviço: payment-service.

## Criação de pagamento

Envie POST /payments com amount, currency e customerId. A moeda aceita neste
ambiente de demonstração é BRL. Envie o cabeçalho Idempotency-Key com um valor
único por operação. Ao repetir uma operação, preserve a mesma chave e o mesmo corpo.

## Retry

O cliente pode repetir a chamada quando receber HTTP 429, 502, 503 ou 504.
São permitidas até três novas tentativas depois da chamada inicial, com espera
de 1, 2 e 4 segundos. Se houver Retry-After, respeite esse valor como espera mínima.
Preserve a Idempotency-Key nas novas tentativas. Não faça retry automático para
HTTP 400, 401, 403 ou 422.

## Timeout com resultado desconhecido

Se a conexão expirar depois do envio, consulte GET /payments/{paymentId} quando
houver um identificador. Se não houver, repita usando a mesma Idempotency-Key.
Não crie uma chave nova apenas porque ocorreu timeout.

## Disponibilidade

Este documento descreve regras da API. O status atual de payment-service deve ser
consultado em uma fonte dinâmica; ele não pode ser inferido destas instruções.
