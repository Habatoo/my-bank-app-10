# Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process

minikube delete

minikube start --driver=docker --memory=8g --cpus=4
minikube addons enable ingress
kubectl create namespace dev

& minikube -p minikube docker-env --shell powershell | Invoke-Expression

echo "--- Building images ---"
docker build -t account:0.1.0 ./account
docker build -t cash:0.1.0 ./cash
docker build -t front-ui:0.1.0 ./front-ui
docker build -t gateway:0.1.0 ./gateway
docker build -t notification:0.1.0 ./notification
docker build -t transfer:0.1.0 ./transfer

echo "--- Deploying with Helm ---"

echo "Шаг 1: Запуск баз данных..."
helm upgrade --install bank-dev ./helm/my-bank/ -n dev -f ./helm/my-bank/tags.yaml --set global.deployDatabases=true --set global.deployKeycloak=false --set global.deployServices=false

kubectl wait --for=condition=ready pod -l "app.kubernetes.io/component=database" -n dev --timeout=300s

echo "Шаг 2: Запуск Keycloak..."
helm upgrade --install bank-dev ./helm/my-bank/ -n dev -f ./helm/my-bank/tags.yaml --set global.deployDatabases=true --set global.deployKeycloak=true --set global.deployServices=false
kubectl wait --for=condition=ready pod -l "app=keycloak" -n dev --timeout=300s

echo "Шаг 3: Запуск микросервисов..."
helm upgrade --install bank-dev ./helm/my-bank/ -n dev -f ./helm/my-bank/tags.yaml --set global.deployDatabases=true --set global.deployKeycloak=true --set global.deployServices=true --wait

& minikube -p minikube docker-env -u --shell powershell | Invoke-Expression

echo "--- Done! ---"
kubectl get pods -n dev