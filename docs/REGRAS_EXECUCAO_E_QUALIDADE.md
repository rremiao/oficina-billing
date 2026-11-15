# Regras de Execução e Qualidade

Adaptação para o OS Service (Java 21, Spring Boot, Maven) das regras de execução e qualidade usadas
no projeto Turmiza. A estrutura e os critérios são os mesmos; as ferramentas são as equivalentes do
ecossistema Java.

## 1. Aplicação obrigatória

Estas regras valem para toda implementação, correção, refatoração ou alteração deste repositório.
Nenhuma entrega é considerada concluída enquanto os critérios aplicáveis não forem verificados.

## 2. Fluxo obrigatório de trabalho

Para cada alteração:

1. Identificar o escopo e a etapa atual (plano de etapas da proposta de escopo do OS Service).
2. Implementar somente o que foi autorizado.
3. Criar ou atualizar testes para toda funcionalidade ou correção de comportamento.
4. Rodar `scripts/quality.sh`, que executa formatação, lint, análise estática, testes, cobertura e
   varredura de segurança.
5. Corrigir os erros encontrados e repetir as verificações até a aprovação.
6. Registrar o Índice de Qualidade do Código (IQC), calculado automaticamente pelo script.
7. Encerrar a entrega informando:
   - resumo do que foi feito;
   - arquivos criados e alterados;
   - comandos e testes executados, com seus resultados;
   - IQC obtido;
   - instruções atualizadas para subir o ambiente.

Alterações exclusivamente documentais não exigem teste funcional, mas exigem verificação de
consistência, links, formatação e recálculo dos critérios aplicáveis do IQC.

## 3. Testes e correções

- Toda nova funcionalidade deve ter testes automatizados proporcionais ao risco.
- Toda correção de defeito deve incluir teste de regressão.
- Fluxos críticos devem ter testes de integração com banco de dados real (Testcontainers com
  PostgreSQL). Mocks não substituem esse teste.
- Concorrência, idempotência, autorização, transições de estado e limites de tempo devem ser
  testados explicitamente. No OS Service isso inclui cada transição permitida e proibida da máquina
  de estados da OS e, a partir da etapa 4, o consumo repetido de mensagens.
- Mudança de comportamento do fluxo da OS visível para o negócio (novo status, nova saída da Saga)
  atualiza os cenários BDD em `src/test/resources/features`, escritos em português e na linguagem
  da oficina, não da implementação.
- Nenhuma implementação pode ser entregue com testes ou verificações conhecidas falhando.
- Erros encontrados dentro do escopo devem ser corrigidos antes da entrega. Problemas externos ou
  fora do escopo devem ser documentados e apresentados como impedimento.

## 4. Índice de Qualidade do Código (IQC)

O índice vai de 0 a 100, inspirado nos quality gates do SonarQube. A entrega exige **IQC igual ou
superior a 90** e ausência de bloqueadores.

| Critério | Peso | Condição para pontuação total | Ferramenta |
|---|---:|---|---|
| Testes automatizados | 25 | Todos os testes aprovados | JUnit 5, Mockito, Testcontainers |
| Cobertura do código alterado | 20 | Pelo menos 90% das linhas novas ou alteradas em `src/main/java` (em relação à `main`) | JaCoCo |
| Análise estática | 20 | Nenhum achado no limiar configurado | SpotBugs + FindSecBugs |
| Estilo e lint | 15 | Formatação e lint sem violações | Spotless + Checkstyle |
| Segurança e confiabilidade | 15 | Nenhuma vulnerabilidade HIGH/CRITICAL, segredo versionado ou configuração insegura | Trivy |
| Documentação e manutenção | 5 | README e `.env.example` compatíveis com a configuração | `scripts/iqc.py` |

Cada critério recebe pontuação proporcional quando mensurável. Critérios não aplicáveis (por
exemplo, nenhuma linha executável alterada) são excluídos e os pesos restantes são normalizados
para 100.

São bloqueadores, independentemente da nota:

- teste falhando;
- cobertura global de linhas abaixo de 80% (exigência do Tech Challenge para cada serviço);
- vulnerabilidade HIGH ou CRITICAL conhecida e não tratada;
- segredo ou credencial real versionada;
- achado de análise estática;
- funcionalidade sem teste;
- migration irreversível ou inconsistente sem tratamento documentado;
- violação conhecida de autorização, integridade ou regras de domínio.

Os quatro primeiros e a análise estática são verificados automaticamente. Os três últimos dependem
de revisão e devem ser confirmados no pull request.

### Comando único

```bash
scripts/quality.sh
```

Requer Java 21 e Docker (Testcontainers e Trivy). O script executa, nesta ordem:

| Etapa | Comando | Relatório |
|---|---|---|
| Testes e cobertura | `./mvnw clean verify` (o JaCoCo derruba o build abaixo de 80%) | `target/surefire-reports`, `target/site/jacoco` |
| Estilo | `./mvnw spotless:check` (corrigir com `./mvnw spotless:apply`) | saída do comando |
| Lint | `./mvnw checkstyle:check` (regras em `config/checkstyle/checkstyle.xml`) | `target/checkstyle-result.xml` |
| Análise estática | `./mvnw spotbugs:check` (exclusões justificadas em `config/spotbugs/exclude.xml`) | `target/spotbugsXml.xml` |
| Dependências | Trivy sobre o JAR gerado, que é o que vai para produção | `target/quality/trivy-dependencias.json` |
| Segredos e configuração | Trivy sobre o repositório | `target/quality/trivy-repositorio.json` |
| IQC | `scripts/iqc.py` | `target/quality/iqc.md` |

O script sai com código diferente de zero se houver bloqueador ou se o IQC ficar abaixo de 90.
Relatórios ficam em `target/` e não são versionados.

### Exclusões de análise estática

Toda exclusão em `config/spotbugs/exclude.xml` precisa de justificativa escrita no próprio arquivo.
Exclusão sem justificativa conta como achado não tratado.

## 5. Controle das etapas

- Trabalhar somente na etapa autorizada pelo responsável do projeto.
- Ao concluir uma etapa, apresentar resultados, arquivos, testes, IQC e instruções de execução.
- Não iniciar a etapa seguinte sem autorização prévia e explícita.
- Uma autorização para corrigir problemas da etapa atual não autoriza antecipar funcionalidades
  futuras.

## 6. Ambiente executável

O projeto deve continuar executável ao final de cada implementação. O `README.md` e o
`.env.example` devem acompanhar mudanças de infraestrutura ou configuração. A entrega deve informar,
no mínimo, como preparar as variáveis, construir os containers, iniciar os serviços, aplicar as
migrations (Flyway, automático na subida) e rodar os testes.

## 7. Padrões permanentes da API

- Erros respondem em `application/problem+json` (`ProblemDetail`), inclusive os gerados pelo próprio
  Spring MVC: 400 para validação, JSON malformado ou tipo inválido na URL, 403 para acesso negado,
  404 para recurso ou rota inexistente, 405 para método não suportado, 422 para regra de negócio.
- Autorização por papel: `ADMIN` acessa tudo, `ATENDENTE` tudo menos `/usuarios`, e o token de
  cliente só as próprias OS (OS de outro cliente responde 404). Toda rota nova nasce restrita a
  operadores; abrir uma rota ao cliente exige conferir a posse no serviço e teste de integração.
- Todo endpoint é documentado no Swagger (`@Tag` no controller, `@Operation` no método) e tem
  request na collection do Postman (`docs/postman`); o `ColecaoPostmanCobreEndpointsTest` garante a
  cobertura da collection.
- Toda entrada é validada no servidor com Bean Validation, com mensagem em português.
- Valores monetários usam `BigDecimal` com duas casas (`NUMERIC(10,2)` no banco).
- Toda mudança de status da OS passa pela máquina de estados (`OrdemServicoStatusService`) e grava
  histórico com a origem da mudança.
- Nenhum acesso ao banco de outro serviço: referências a outros serviços são ids lógicos, sem FK.

## 8. Documentação e evidências

- O README mantém a tabela "Entregáveis do Tech Challenge" apontando para onde cada exigência do PDF
  está no repositório; item novo ou movido atualiza a tabela.
- Diagrama de arquitetura vive em `docs/arquitetura/*.mmd` (fonte) e é repetido no README; mudar a
  arquitetura muda os dois. Decisão de arquitetura nova vira ADR em `docs/adr`.
- As evidências (prints de cobertura, BDD e Swagger, e os PNGs dos diagramas) são regeneradas com
  `scripts/gerar-evidencias.sh` depois do `scripts/quality.sh`, e os números da tabela de evidências
  do README vêm do `target/quality/iqc.md` da mesma execução.
- Todo link relativo e âncora da documentação precisa resolver.

## 9. Entrega contínua

- A `main` só recebe código por pull request com os checks Qualidade, Imagem e Manifestos verdes; o
  deploy no EKS acontece só pelo pipeline, depois do merge.
- Mudança em manifesto é validada no kind (`scripts/k8s-local.sh`) antes do PR, com o mesmo
  `scripts/deploy-k8s.sh` usado pelo pipeline.
- Segredo nunca entra em manifesto versionado: vai como GitHub Secret e o pipeline cria o Secret do
  Kubernetes.

## 10. Padrões permanentes da mensageria

- Evento ou comando para outro serviço só pelo `OutboxService`, dentro da transação da mudança que o
  provocou. Publicar direto no `RabbitTemplate` a partir do domínio perde o evento se o broker cair.
- Toda reação a mensagem recebida é um `ManipuladorEvento`, chamado pelo `ProcessadorEventos`: a
  deduplicação pelo `messageId` e o envio para a DLQ vêm de graça. O manipulador lança
  `RegraNegocioException` ou `MensagemInvalidaException` quando a mensagem nunca vai poder ser
  processada, e qualquer outra exceção quando a falha é transitória.
- Todo manipulador novo tem teste de consumo repetido (a segunda entrega não pode ter efeito).
- Toda reação a evento da Saga passa pelo `OrquestradorSaga`, que trava a Saga da OS e confere se a
  etapa espera aquele evento. A checagem de status da OS sozinha não basta: durante um estorno a OS
  continua em `APROVADA` (ou `AGUARDANDO_ITENS`), que aceitaria `ExecucaoIniciada`.
- Toda saída de compensação nova tem teste de integração com Billing e Execução simulados
  (`SagaIntegrationTest`).
- Mudança no envelope ou nos `dados` de um tipo publicado é mudança de contrato: atualizar
  `docs/mensageria.md` e avisar os times consumidores.

## 11. Arquitetura e SOLID (Billing Service)

O Billing segue Clean Architecture / Ports and Adapters, como combinado no documento de divisão dos
três microsserviços, e os princípios SOLID. Esta seção vale para todo código novo ou alterado.

### Camadas e regra de dependência

| Camada | Contém | Pode depender de |
|---|---|---|
| Domínio | Orçamento, pagamento, estorno, catálogo e as regras deles | nada fora do domínio |
| Aplicação | Casos de uso e as portas (interfaces) que eles usam | domínio |
| Adaptadores de entrada | Controllers REST, listeners de mensagem, webhook do Mercado Pago | aplicação |
| Adaptadores de saída | Persistência (JPA), cliente do Mercado Pago (Feign), publicação no RabbitMQ (outbox) | aplicação e domínio |

- As dependências apontam sempre para dentro: domínio e casos de uso não importam Spring Web, JPA,
  Feign, RabbitMQ nem DTOs do Mercado Pago.
- Entidades JPA ficam no adaptador de persistência, separadas das classes de domínio.
- DTOs do Mercado Pago nunca saem do adaptador do Mercado Pago. Controller e evento expõem modelos
  próprios do Billing (hoje o `BillingController` devolve `MercadoPagoOrderResponse` direto, e isso
  é corrigido na etapa 1).
- Injeção de dependência só por construtor.

### Os princípios no Billing

- **S — Responsabilidade única.** Um caso de uso por classe (`GerarOrcamento`, `CobrarPagamento`,
  `EstornarPagamento`, `ConfirmarPagamento`). Controller e listener só traduzem a entrada e chamam o
  caso de uso; o adaptador do Mercado Pago só traduz entre o domínio e a API.
- **O — Aberto/fechado.** Novo meio de pagamento ou novo gateway entra como um adaptador novo que
  implementa a porta existente, sem mexer no domínio nem nos casos de uso. Novo comando consumido
  entra como um manipulador novo, sem mexer no despacho das mensagens.
- **L — Substituição de Liskov.** Toda implementação de uma porta respeita o contrato dela: as mesmas
  exceções, a mesma idempotência e nada de `null` onde a porta promete exceção. O gateway falso usado
  nos testes se comporta como o real; se não se comportar, o teste não prova nada.
- **I — Segregação de interfaces.** Portas pequenas, por necessidade do caso de uso (por exemplo,
  cobrar e consultar pagamento separado de estornar), em vez de uma interface única espelhando a API
  do Mercado Pago. O caso de uso de orçamento não depende de nenhuma porta de pagamento.
- **D — Inversão de dependência.** A aplicação define as portas e os adaptadores as implementam. O
  domínio não sabe que existem Mercado Pago, PostgreSQL ou RabbitMQ.

### Como é verificado

- A regra de dependência entre camadas é coberta por um teste de arquitetura (ArchUnit) a partir da
  etapa 1. Violação derruba o build e conta como bloqueador.
- Os princípios SOLID que o teste não pega (responsabilidade única, portas pequenas, contrato
  respeitado) são conferidos na revisão do pull request.
