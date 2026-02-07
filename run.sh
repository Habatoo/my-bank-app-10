#!/bin/bash

set -e

echo "--- Cleaning up ---"
minikube delete

echo "--- Starting Minikube ---"
minikube start --driver=docker --memory=8g --cpus=4
minikube addons enable ingress

echo "--- Creating Namespace ---"
kubectl create namespace dev

eval $(minikube -p minikube docker-env)

echo "--- Building images ---"
docker build -t account:0.1.0 ./account
docker build -t cash:0.1.0 ./cash
docker build -t front-ui:0.1.0 ./front-ui
docker build -t gateway:0.1.0 ./gateway
docker build -t notification:0.1.0 ./notification
docker build -t transfer:0.1.0 ./transfer

echo "--- Deploying with Helm ---"

echo "Шаг 1: Запуск баз данных..."
helm upgrade --install bank-dev ./helm/my-bank/ -n dev -f ./helm/my-bank/tags.yaml \
  --set global.deployDatabases=true \
  --set global.deployKeycloak=false \
  --set global.deployServices=false

echo "Ожидание готовности баз данных..."
kubectl wait --for=condition=ready pod -l "app.kubernetes.io/component=database" -n dev --timeout=300s

echo "Шаг 2: Запуск Keycloak..."
helm upgrade --install bank-dev ./helm/my-bank/ -n dev -f ./helm/my-bank/tags.yaml \
  --set global.deployDatabases=true \
  --set global.deployKeycloak=true \
  --set global.deployServices=false

echo "Ожидание готовности Keycloak..."
kubectl wait --for=condition=ready pod -l "app=keycloak" -n dev --timeout=300s

echo "Шаг 3: Запуск микросервисов..."
helm upgrade --install bank-dev ./helm/my-bank/ -n dev -f ./helm/my-bank/tags.yaml \
  --set global.deployDatabases=true \
  --set global.deployKeycloak=true \
  --set global.deployServices=true --wait

eval $(minikube -p minikube docker-env -u)

echo "--- Done! ---"
kubectl get pods -n dev