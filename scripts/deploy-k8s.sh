#!/usr/bin/env bash
# Publica o Billing Service num cluster Kubernetes a partir de um overlay de k8s/overlays.
# Usado pelo pipeline (overlay eks) e pela validação local no kind (overlay local, scripts/k8s-local.sh).
#
# Uso: scripts/deploy-k8s.sh <overlay> <namespace> <imagem>
#   ex.: scripts/deploy-k8s.sh eks oficina 123456789012.dkr.ecr.us-east-1.amazonaws.com/oficina-billing:abc123
#
# Com CRIAR_SECRETS=true, cria o Secret a partir destas variáveis de ambiente (no pipeline vêm dos
# GitHub Secrets; nunca são versionadas):
#   BILLING_DATASOURCE_URL, BILLING_DATASOURCE_USERNAME, BILLING_DATASOURCE_PASSWORD, SECURITY_JWT_SECRET,
#   RABBITMQ_USERNAME, RABBITMQ_PASSWORD, MERCADOPAGO_ACCESS_TOKEN, MERCADOPAGO_WEBHOOK_SECRET
#
# O RabbitMQ não é implantado aqui no EKS: é o do OS Service, no mesmo namespace, e precisa estar no ar.
set -euo pipefail

OVERLAY="${1:?informe o overlay (eks ou local)}"
NAMESPACE="${2:?informe o namespace}"
IMAGEM="${3:?informe a imagem completa, com tag}"
TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"
APP=billing-service

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
DIR_OVERLAY="$RAIZ/k8s/overlays/$OVERLAY"
[ -d "$DIR_OVERLAY" ] || { echo "Overlay inexistente: $DIR_OVERLAY" >&2; exit 1; }

diagnostico() {
	echo "::group::Diagnóstico do namespace $NAMESPACE"
	kubectl get all,hpa -n "$NAMESPACE" || true
	kubectl get events -n "$NAMESPACE" --sort-by=.metadata.creationTimestamp | tail -30 || true
	kubectl describe deployment "$APP" -n "$NAMESPACE" | tail -40 || true
	kubectl logs "deployment/$APP" -n "$NAMESPACE" --tail=100 || true
	echo "::endgroup::"
}
trap 'diagnostico' ERR

# Overlay temporário que só troca a imagem: kubectl kustomize resolve, sem depender do binário kustomize
TEMP="$RAIZ/k8s/overlays/.deploy-$$"
trap 'rm -rf "$TEMP"' EXIT
mkdir -p "$TEMP"
cat > "$TEMP/kustomization.yaml" <<YAML
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../$OVERLAY
images:
  - name: oficina-billing
    newName: ${IMAGEM%:*}
    newTag: ${IMAGEM##*:}
# Vai no mesmo apply da imagem, para o histórico do rollout registrar a versão certa
patches:
  - target:
      kind: Deployment
      name: $APP
    patch: |-
      - op: add
        path: /metadata/annotations
        value:
          kubernetes.io/change-cause: "imagem $IMAGEM"
YAML

echo "==> namespace $NAMESPACE"
kubectl apply -f "$DIR_OVERLAY/namespace.yaml"

REINICIAR=false
if [ "${CRIAR_SECRETS:-false}" = "true" ]; then
	FALTANDO=""
	for variavel in BILLING_DATASOURCE_URL BILLING_DATASOURCE_USERNAME BILLING_DATASOURCE_PASSWORD \
		SECURITY_JWT_SECRET RABBITMQ_USERNAME RABBITMQ_PASSWORD MERCADOPAGO_ACCESS_TOKEN MERCADOPAGO_WEBHOOK_SECRET; do
		[ -n "${!variavel:-}" ] || FALTANDO="$FALTANDO $variavel"
	done
	if [ -n "$FALTANDO" ]; then
		echo "::error::Secrets não configurados no GitHub:$FALTANDO (ver README, seção CI/CD)" >&2
		exit 1
	fi

	echo "==> secret"
	SAIDA=$(kubectl create secret generic billing-service-secret --namespace "$NAMESPACE" \
		--from-literal=SPRING_DATASOURCE_URL="$BILLING_DATASOURCE_URL" \
		--from-literal=SPRING_DATASOURCE_USERNAME="$BILLING_DATASOURCE_USERNAME" \
		--from-literal=SPRING_DATASOURCE_PASSWORD="$BILLING_DATASOURCE_PASSWORD" \
		--from-literal=SECURITY_JWT_SECRET="$SECURITY_JWT_SECRET" \
		--from-literal=SPRING_RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
		--from-literal=SPRING_RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
		--from-literal=MERCADOPAGO_ACCESS_TOKEN="$MERCADOPAGO_ACCESS_TOKEN" \
		--from-literal=MERCADOPAGO_WEBHOOK_SECRET="$MERCADOPAGO_WEBHOOK_SECRET" \
		--dry-run=client --output yaml | kubectl apply -f -)
	echo "$SAIDA"
	# Pod não relê Secret alterado: se mudou, a aplicação precisa ser reiniciada
	if echo "$SAIDA" | grep -q "configured"; then
		REINICIAR=true
	fi
fi

echo "==> manifestos ($OVERLAY) com a imagem $IMAGEM"
kubectl apply -k "$TEMP"

if [ "$REINICIAR" = "true" ] && kubectl get deployment "$APP" -n "$NAMESPACE" >/dev/null 2>&1; then
	echo "==> secret alterado: reiniciando o $APP"
	kubectl rollout restart "deployment/$APP" -n "$NAMESPACE"
fi

echo "==> aguardando o rollout"
# No EKS o RabbitMQ é o do OS Service; sem ele no namespace, o deploy para aqui com mensagem clara
if ! kubectl get statefulset/rabbitmq -n "$NAMESPACE" >/dev/null 2>&1; then
	echo "::error::RabbitMQ não encontrado no namespace $NAMESPACE: implante o OS Service antes" >&2
	exit 1
fi
kubectl rollout status statefulset/rabbitmq -n "$NAMESPACE" --timeout="$TIMEOUT"
kubectl rollout status "deployment/$APP" -n "$NAMESPACE" --timeout="$TIMEOUT"

echo "==> conferindo a imagem em execução"
APLICADA=$(kubectl get deployment "$APP" -n "$NAMESPACE" \
	-o jsonpath="{.spec.template.spec.containers[?(@.name==\"$APP\")].image}")
if [ "$APLICADA" != "$IMAGEM" ]; then
	echo "::error::Deployment com a imagem $APLICADA, esperada $IMAGEM" >&2
	exit 1
fi
# Pods do ReplicaSet antigo ainda em encerramento aparecem como Running por alguns segundos: só contam
# os que não estão saindo
DIFERENTES=$(kubectl get pods -n "$NAMESPACE" -l "app.kubernetes.io/name=$APP" \
	--field-selector=status.phase=Running \
	-o jsonpath='{range .items[*]}{.metadata.deletionTimestamp}{"|"}{.spec.containers[0].image}{"\n"}{end}' \
	| grep '^|' | cut -d'|' -f2 | grep -vx "$IMAGEM" || true)
if [ -n "$DIFERENTES" ]; then
	echo "::error::Pods rodando outra imagem: $DIFERENTES" >&2
	exit 1
fi

echo "Deploy concluído: $IMAGEM"
