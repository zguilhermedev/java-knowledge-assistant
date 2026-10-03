# API de recarga

Documentação fictícia para desenvolvimento e avaliação de recuperação.
Identificador do documento: recharge-api. Serviço: recharge-service.

## Solicitação

Envie POST /recharges com phoneNumber e amount. Informe o telefone com código do
país e DDD. O valor mínimo de uma recarga neste ambiente é R$ 10,00.

## Acompanhamento

Uma solicitação aceita retorna HTTP 202 e rechargeId. Consulte
GET /recharges/{rechargeId} para acompanhar PENDING, COMPLETED ou FAILED.
Uma resposta 202 indica aceitação, não conclusão da recarga.

## Falhas

Se o número for inválido, a API retorna HTTP 422. Corrija o telefone antes de
tentar novamente. Quando a solicitação já tiver rechargeId, consulte seu status
antes de criar outra recarga para o mesmo cliente.
