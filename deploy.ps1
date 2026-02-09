param (
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "test", "prod")]
    [string]$Environment,
    [switch]$CleanStart
)

$ReleaseName = "bank-$Environment"
$Namespace = $Environment
$HelmPath = "./helm/my-bank"
$TagsPath = "$HelmPath/tags.yaml"

if ($CleanStart) {
    Write-Host "Action: Recreating Minikube"
    minikube delete
    minikube start --driver=docker --memory=8g --cpus=4
    minikube addons enable ingress
}

if (-not (kubectl get ns $Namespace --ignore-not-found)) {
    kubectl create namespace $Namespace
}

& minikube -p minikube docker-env --shell powershell | Invoke-Expression

Write-Host "Step 1: Deploying Databases"
Run-DeployStep "true" "false" "false"
Start-Sleep -Seconds 5
kubectl wait --for=condition=ready pod -l "app.kubernetes.io/component=database" -n $Namespace --timeout=300s

Write-Host "Step 2: Deploying Keycloak"
Run-DeployStep "true" "true" "false"
Start-Sleep -Seconds 5
kubectl wait --for=condition=ready pod -l "app=keycloak" -n $Namespace --timeout=300s

Write-Host "Action: Building images"
$services = @("account", "cash", "front-ui", "gateway", "notification", "transfer")
foreach ($svc in $services) {
    docker build -t "${svc}:0.1.0" "./$svc"
}

function Run-DeployStep {
    param($Db, $Kc, $Svc)

    $Command = "helm upgrade --install $ReleaseName $HelmPath -n $Namespace " +
               "-f $HelmPath/values.yaml " +
               "-f $TagsPath " +
               "-f $HelmPath/values-$Environment.yaml " +
               "--set global.deployDatabases=$Db " +
               "--set global.deployKeycloak=$Kc " +
               "--set global.deployServices=$Svc " +
               "--set account.image.pullPolicy=Never " +
               "--set cash.image.pullPolicy=Never " +
               "--set front-ui.image.pullPolicy=Never " +
               "--set gateway.image.pullPolicy=Never " +
               "--set notification.image.pullPolicy=Never " +
               "--set transfer.image.pullPolicy=Never"

    Invoke-Expression $Command
}

Write-Host "Step 3: Deploying Services"
Run-DeployStep "true" "true" "true"

& minikube -p minikube docker-env -u --shell powershell | Invoke-Expression

Write-Host "Status: Deployment completed"
kubectl get pods -n $Namespace