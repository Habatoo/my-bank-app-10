#!/bin/bash

ENVIRONMENT=$1
CLEAN_START=$2

if [[ ! "$ENVIRONMENT" =~ ^(dev|test|prod)$ ]]; then
    echo "Usage: ./deploy.sh [dev|test|prod] [--clean]"
    exit 1
fi

RELEASE_NAME="bank-$ENVIRONMENT"
NAMESPACE=$ENVIRONMENT
HELM_PATH="./helm/my-bank"
TAGS_PATH="$HELM_PATH/tags.yaml"

if [[ "$CLEAN_START" == "--clean" ]]; then
    echo "Action: Recreating Minikube"
    minikube delete
    minikube start --driver=docker --memory=8g --cpus=4
    minikube addons enable ingress
fi

if ! kubectl get ns "$NAMESPACE" > /dev/null 2>&1; then
    kubectl create namespace "$NAMESPACE"
fi

eval $(minikube -p minikube docker-env)

echo "Action: Building images"
services=("account" "cash" "front-ui" "gateway" "notification" "transfer")
for svc in "${services[@]}"; do
    docker build -t "${svc}:0.1.0" "./$svc"
done

run_deploy_step() {
    local db=$1
    local kc=$2
    local svc=$3

    helm upgrade --install "$RELEASE_NAME" "$HELM_PATH" -n "$NAMESPACE" \
        -f "$HELM_PATH/values.yaml" \
        -f "$TAGS_PATH" \
        -f "$HELM_PATH/values-$ENVIRONMENT.yaml" \
        --set global.deployDatabases="$db" \
        --set global.deployKeycloak="$kc" \
        --set global.deployServices="$svc" \
        --set account.image.pullPolicy=Never \
        --set cash.image.pullPolicy=Never \
        --set front-ui.image.pullPolicy=Never \
        --set gateway.image.pullPolicy=Never \
        --set notification.image.pullPolicy=Never \
        --set transfer.image.pullPolicy=Never
}

echo "Step 1: Deploying Databases"
run_deploy_step "true" "false" "false"
sleep 5
kubectl wait --for=condition=ready pod -l "app.kubernetes.io/component=database" -n "$NAMESPACE" --timeout=300s

echo "Step 2: Deploying Keycloak"
run_deploy_step "true" "true" "false"
sleep 5
kubectl wait --for=condition=ready pod -l "app=keycloak" -n "$NAMESPACE" --timeout=300s

echo "Step 3: Deploying Services"
run_deploy_step "true" "true" "true"

eval $(minikube -p minikube docker-env -u)

echo "Status: Deployment completed"
kubectl get pods -n "$NAMESPACE"