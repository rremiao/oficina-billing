# language: pt
Funcionalidade: Orçamento e pagamento conduzidos pela Saga
  Como oficina, quero que o Billing monte o orçamento com os preços do catálogo, cobre o cliente pelo
  Mercado Pago quando ele aprovar e devolva o dinheiro se o reparo não puder ser feito, respondendo
  sempre ao OS Service, para que a ordem de serviço avance sem atualização manual.

  Contexto:
    Dado o catálogo com o serviço "Troca de pastilhas" a R$ 150,00 e a peça "Pastilha de freio" a R$ 89,90
    Quando o OS Service pede o orçamento da OS 42 com o serviço e 2 unidades da peça
    Então o Billing responde que o orçamento foi gerado com total de R$ 329,80

  Cenário: Cliente aprova e paga pelo Mercado Pago
    Quando o OS Service pede a cobrança do orçamento
    Então o cliente encontra o link de pagamento do Mercado Pago
    Quando o Mercado Pago avisa que o pagamento foi aprovado por "pix"
    Então o Billing responde que o pagamento de R$ 329,80 foi confirmado

  Cenário: Reparo falha depois do pagamento e o dinheiro volta para o cliente
    Dado que o cliente já pagou o orçamento
    Quando o OS Service pede o estorno do pagamento porque "Peça danificada na montagem"
    Então o Billing responde que R$ 329,80 foram estornados
    E o Mercado Pago recebeu um único pedido de reembolso

  Cenário: Cliente não paga e a cobrança expira
    Quando o OS Service pede a cobrança do orçamento
    E o Mercado Pago avisa que a cobrança expirou
    Então o Billing responde que o pagamento foi recusado

  Cenário: O mesmo pedido de orçamento chega duas vezes
    Quando o OS Service pede de novo o mesmo orçamento
    Então o Billing responde de novo com a mesma mensagem
    E existe um único orçamento para a OS 42
