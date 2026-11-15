package br.com.oficina.billing.shared.exception;

public final class ExceptionMessage {

	public static final String SERVICE_NOT_FOUND = "Serviço não encontrado";
	public static final String ITEM_NOT_FOUND = "Item não encontrado";
	public static final String EXISTING_SERVICE = "Já existe serviço cadastrado com este nome";
	public static final String EXISTING_ITEM = "Já existe item cadastrado com o mesmo tipo, nome e unidade de medida";
	public static final String REQUIRED_NAME = "Nome é obrigatório";
	public static final String REQUIRED_TYPE = "Tipo é obrigatório";
	public static final String REQUIRED_UNIT = "Unidade de medida é obrigatória";
	public static final String INVALID_AMOUNT = "%s deve ser maior que zero";
	public static final String INVALID_AMOUNT_SCALE = "%s deve ter no máximo duas casas decimais";
	public static final String AMOUNT_OVER_LIMIT = "%s passa do limite de 99.999.999,99";
	public static final String QUOTE_NOT_FOUND = "Orçamento não encontrado";
	public static final String QUOTE_WITHOUT_LINES = "O orçamento precisa de ao menos um serviço ou item";
	public static final String REQUIRED_SAGA = "O pedido de orçamento precisa do sagaId";
	public static final String REQUIRED_SERVICE_ORDER = "O pedido de orçamento precisa da ordem de serviço";
	public static final String REQUIRED_CUSTOMER = "O pedido de orçamento precisa do cliente";
	public static final String REQUIRED_CATALOG_ID = "Toda linha do orçamento precisa do id do catálogo";
	public static final String INVALID_QUANTITY = "Quantidade do item %d deve ser maior que zero";
	public static final String SERVICE_NOT_IN_CATALOG = "Serviço %d não existe no catálogo";
	public static final String ITEM_NOT_IN_CATALOG = "Item %d não existe no catálogo";
	public static final String INACTIVE_SERVICE = "Serviço %d está inativo e não entra em orçamento";
	public static final String INACTIVE_ITEM = "Item %d está inativo e não entra em orçamento";
	public static final String SAGA_OF_OTHER_ORDER = "O sagaId %s já gerou orçamento para outra ordem de serviço";
	public static final String PAYMENT_NOT_FOUND = "Pagamento não encontrado";
	public static final String QUOTE_TO_CHARGE_NOT_FOUND = "Orçamento %d não existe para ser cobrado";
	public static final String QUOTE_OF_OTHER_SAGA = "O orçamento %d é de outro ciclo da Saga";
	public static final String PAYMENT_TO_REFUND_NOT_FOUND = "Pagamento %d não existe para ser estornado";
	public static final String PAYMENT_OF_OTHER_SAGA = "O pagamento %d é de outro ciclo da Saga";
	public static final String REFUND_NOT_ALLOWED = "Só pagamento confirmado pode ser estornado (pagamento %d está %s)";

	private ExceptionMessage() {
	}
}
