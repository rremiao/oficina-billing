# ADR 0004 — Deploy no EKS com o broker do OS Service

- **Status:** aceita
- **Data:** 2026-11-14

## Contexto

O Billing roda no mesmo cluster EKS do time (`oficina-eks`, namespace `oficina`) que o OS Service. A
Saga só funciona se os dois usarem o mesmo RabbitMQ, e o OS Service já implanta um StatefulSet
`rabbitmq` nesse namespace (componente `k8s/componentes/rabbitmq` dele).

## Decisão

- **Mesmos manifestos e o mesmo pipeline do OS Service.** Kustomize com base comum e overlays `eks` e
  `local`. O pipeline tem os checks Qualidade, Imagem e Manifestos e faz o deploy no EKS só depois do
  merge, com imagem imutável por commit no ECR e smoke test no Load Balancer.
- **No EKS, o Billing não implanta RabbitMQ:** usa o Service `rabbitmq` do namespace, com as mesmas
  credenciais. Um segundo StatefulSet com o mesmo nome sobrescreveria o do OS Service. O
  `scripts/deploy-k8s.sh` para com erro claro se o broker não estiver no namespace.
- **No kind (overlay `local`)**, o Billing sobe PostgreSQL e RabbitMQ próprios, para validar os
  manifestos sozinho, num cluster `oficina-billing` separado do cluster do OS Service.
- **Exposição por Load Balancer**, como o OS Service: o API Gateway encaminha `/billing/v1/**`. O webhook
  do Mercado Pago (`/billing/v1/pagamentos/webhook/mercadopago`) precisa de rota sem o authorizer JWT.
- **Segredos só como GitHub Secrets**, transformados pelo pipeline no Secret `billing-service-secret`.

## Consequências

- O OS Service precisa ser implantado antes do Billing num cluster novo.
- Falta, fora deste repositório: o RDS `oficina-billing-db` no `oficina-database` e a rota do Billing,
  com o webhook fora do authorizer, no `oficina-lambda`.
- O RabbitMQ do OS Service usa `emptyDir` (o Learner Lab não tem volume persistente): mensagem parada na
  fila se perde se o pod do broker for recriado. Os eventos ainda não enviados continuam no outbox.
