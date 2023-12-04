if ($args.Length -eq 0) {
    $studentNumber = Read-Host "Enter student number"
} else {
    $studentNumber = $args[0]
}

$propertiesContent = (Get-Content -Path ".\config.properties" -Raw)

if (-not ($propertiesContent -match "^$studentNumber=")) {
    Write-Host "StudentNumber '$studentNumber' does not exist"
    Exit 1
}

$imageName = ($propertiesContent | ConvertFrom-StringData)[$studentNumber]

Write-Host "Executing Maven commands..."
mvn clean compile package

Write-Host "Logging into Docker..."
docker login

Write-Host "Building Docker image..."
docker build -t $imageName ./docker

Write-Host "Pushing Docker image..."
docker push $imageName

# -------------------------------------------- #

Write-Host "Starting Kubernetes..."

$resourceGroupName = "scc2324-cluster-$studentNumber"
$clusterName = "my-scc2324-cluster-$studentNumber"

Write-Host "Creating Azure Resource Group..."
$resourceGroupOutput = az group create --name $resourceGroupName --location westeurope
$resourceGroupId = ($resourceGroupOutput | ConvertFrom-Json).id

Write-Host "Creating Service Principal..."
$serviceOutput = az ad sp create-for-rbac --name http://scc2324-kuber-$studentNumber --role Contributor --scope $resourceGroupId
$serviceJSON = $serviceOutput | ConvertFrom-Json

Write-Host "Creating Cluster..."
$resourceGroupOutput = az aks create --resource-group $resourceGroupName --name $clusterName --node-vm-size Standard_B2s --generate-ssh-keys --node-count 2 --service-principal $serviceJSON.appId  --client-secret $serviceJSON.password

Write-Host "Getting Kubernetes credentials..."
az aks get-credentials --resource-group scc2324-cluster-$studentNumber --name $clusterName

Write-Host "Applying yaml in docker directory..."
kubectl apply -f .\docker\sccapp.yaml

