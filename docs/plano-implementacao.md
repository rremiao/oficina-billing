# Billing Service — plano de implementação (Fase 4)

Levantamento do que falta neste repositório para o Billing atender ao enunciado do Tech Challenge
Fase 4 e ao documento comum do time ("Fase 4 — Divisão dos três microsserviços"), e a ordem
sugerida para implementar.

Estado em 14/11/2026: a branch `release/billing-service` tem o cliente Feign do Mercado Pago, cerca
de 45 DTOs mapeados e um controller. A `main` só tem o README.

## Andamento

| Etapa | Situação |
| --- | --- |
| 1. Fundação | Feita |
| 2. Banco e catálogo | Feita |
| 3. Orçamento | Feita |
| 4. Pagamento e Mercado Pago | Feita (sem validação contra o sandbox, ver ADR 0002) |
| 5. Saga e mensageria | Feita (ADR 0003, contrato em `docs/mensageria.md`) |
| 6. Deploy e entrega | Feita (ADR 0004); o deploy real depende do RDS e da rota no API Gateway |

Ajustes em relação ao plano original, feitos durante a implementação:

- O `docker-compose.yml` cresce junto com as etapas: na 1 só a aplicação, na 2 o PostgreSQL, e o
  RabbitMQ entra na 5, quando for usado. Subir infraestrutura que nada usa só esconderia erro.
- O controller e o service antigos (`BillingController`, `BillingService`) saíram na etapa 1: expunham
  DTO do Mercado Pago na API e chamavam a Orders API com pedido vazio. O cliente Feign e os DTOs foram
  para `pagamento/output/mercadopago`, sem mudança de lógica, e têm teste de contrato JSON.
- O orçamento não tem `POST` na API: nasce pelo caso de uso `GerarOrcamento`, que a etapa 5 liga ao
  comando `SolicitarOrcamento`.

## Responsabilidade do serviço

O Billing é o dono de tudo que tem valor em reais: catálogo com preço, orçamento, pagamento via
Mercado Pago e estorno. Não conhece o Execution Service; fala só com o OS Service, por evento.

## O que já existe e pode ser reaproveitado

- DTOs da Orders API do Mercado Pago (`dto/auth`, `dto/order/create`, `dto/order/find`).
- Interface `BillingClient` (Feign) com `getAccessToken`, `createOrder` e `getOrder`.

## O que falta

### Domínio e banco

- PostgreSQL próprio (`oficina-billing-db`), com Flyway e as tabelas `servico`, `item`,
  `orcamento`, `orcamento_servico`, `orcamento_item` e `pagamento`.
- Catálogo com preço: migrar os pacotes `item/` e `servico/` do monólito.
- Orçamento: cabeçalho com o total e linhas de serviços e peças com `valor_unitario`.
- Pagamento: status, ids do Mercado Pago, histórico de tentativas e payload bruto em `jsonb`, com
  constraint de um único pagamento confirmado por orçamento.
- Estorno como operação de primeira classe (é a compensação principal da Saga).

### Mercado Pago

Resolvido na etapa 4 ([ADR 0002](adr/0002-pagamento-pela-orders-api-do-mercado-pago.md)): autenticação
por access token no lugar do `/oauth/token`, pedido montado a partir do orçamento, chave de
idempotência fixa por orçamento e por estorno, `mercadopago.url` configurado, webhook assinado e
refund. O que estava errado no código herdado era:

- `BillingServiceImpl.getToken()` concatena o objeto `MercadoPagoAuthResponse` inteiro em vez de
  usar o campo `access_token`.
- `MercadoPagoAuthRequest` e `MercadoPagoOrderRequest` são enviados vazios (sem credenciais, valor,
  itens ou pagador).
- `X-Idempotency-Key` está fixo em `"testKey"`; precisa ser único por requisição.
- `billing.url` não está definido em nenhum `application.properties`.

Além das correções, faltam o webhook de confirmação de pagamento e a chamada de refund.

Revisar também nos DTOs, ao ligar o cliente na etapa 4 (o teste de contrato da etapa 1 deixou esses
campos de fora de propósito):

- `MercadoPagoFindOrderConfig.paymentMethod` e `MercadoPagoFindOrderOnline.autoReturn` estão sem
  `@JsonProperty`, então esperam camelCase, enquanto todo o resto está em snake_case.
- Os campos de data são `LocalDateTime`; conferir o formato que a API devolve (com ou sem fuso) antes de
  confiar neles.
- Os DTOs de viagem (`TravelPassenger`, `TravelRoute`) não têm uso numa oficina e podem sair.

### Saga e mensageria (RabbitMQ, igual ao OS Service)

| Direção | Mensagens |
| --- | --- |
| Consome (comandos do OS Service) | `SolicitarOrcamento`, `CobrarPagamento`, `EstornarPagamento` |
| Publica (exchange `billing.eventos`) | `OrcamentoGerado`, `PagamentoConfirmado`, `PagamentoRecusado`, `PagamentoEstornado` |

- Envelope padrão: `sagaId`, `tipo`, `ordemServicoId`, `codigoOrdemServico`, `correlationId`,
  `ocorridoEm`, `dados`.
- Fila própria com DLQ e três tentativas.
- Idempotência por `sagaId`: o consumo é at-least-once, então um comando repetido não pode gerar
  orçamento, cobrança ou estorno duplicado.

### Convenções do time

- Clean Architecture / Ports and Adapters (hoje é controller → service → client direto).
- `context-path` `/billing/v1`, `/actuator/health`, Swagger em `/swagger-ui`.
- Validação do JWT, `CorrelationIdFilter` e CPF mascarado em log (copiar de `shared/` e
  `security/` do OS Service).
- `.gitattributes` com `* text=auto` e `.env.example` com placeholders.

### Testes e qualidade

- Testes unitários (hoje só existe o `BillingApplicationTests` padrão).
- JaCoCo com mínimo de 80% quebrando o build.
- SonarCloud no CI.
- Testes de integração com Testcontainers (Postgres e RabbitMQ) e Mercado Pago mockado (WireMock).

### Infra e entrega

- `Dockerfile` e `docker-compose.yml` local (Postgres, RabbitMQ e app).
- Manifestos Kubernetes (seguir o kustomize do OS Service).
- Pipeline de CI/CD: build, testes, qualidade e deploy no EKS.
- README com arquitetura, evidência de cobertura e Swagger ou collection do Postman.
- Proteção da `main` (PR obrigatório) e merge desta branch.

## Etapas sugeridas

Seis etapas, cada uma em um PR próprio.

1. **Fundação.** Reorganizar o código atual em Clean Architecture; aplicar as convenções
   (`/billing/v1`, actuator, Swagger, `.gitattributes`, `.env.example`); `Dockerfile`,
   `docker-compose` com Postgres e RabbitMQ; CI mínimo com build, testes e JaCoCo 80%; JWT,
   `CorrelationIdFilter` e mascaramento de CPF copiados do OS Service.
2. **Banco e catálogo.** Flyway com `servico` e `item`; migração dos pacotes `item/` e `servico/`
   do monólito com os preços; testes.
3. **Orçamento.** Domínio de `orcamento` com linhas de serviços e itens, cálculo dos totais e
   persistência; testes.
4. **Pagamento e Mercado Pago.** Correção do client (token, request vazio, idempotency key);
   tabela `pagamento` com payload em `jsonb`; webhook de confirmação; refund; testes de integração
   com WireMock.
5. **Saga e mensageria.** Consumo dos comandos e publicação dos eventos; envelope padrão;
   idempotência por `sagaId`; outbox; DLQ; testes de integração com Testcontainers e um cenário BDD
   do fluxo de pagamento.
6. **Deploy e entrega.** Manifestos Kubernetes; pipeline completo (SonarCloud, imagem, deploy no
   EKS); README; collection do Postman; proteção da `main`.

As etapas 3 e 4 podem andar em paralelo depois da 2. A etapa 5 é a que liga o Billing ao OS
Service e destrava o teste de ponta a ponta da Saga.

## Referências

- Enunciado: "Fase 4 - Tech challenge.pdf".
- Contrato entre os serviços: "Fase 4 — Divisão dos três microsserviços", seção "Os contratos
  entre os serviços".
- Implementação de referência para mensageria, outbox, idempotência e pipeline:
  `oficina-os-service` (`mensageria/`, `saga/`, `.github/workflows/pipeline.yml`).
