# Prompt the user for the imageName
$imageName = Read-Host "Enter the Docker image name"

# Check if $imageName is provided
if (-not $imageName) {
    Write-Host "Error: Please provide a valid imageName."
    exit 1
}

# Prompt the user for the studentNumber
$studentNumber = Read-Host "Enter the student number"

# Check if $studentNumber is provided
if (-not $studentNumber) {
    Write-Host "Error: Please provide a valid student number."
    exit 1
}

 # Maven commands
Write-Host "Executing Maven commands..."
mvn clean compile package

# Docker login
Write-Host "Logging into Docker..."
docker login

# Docker build
Write-Host "Building Docker image..."
docker build -t $imageName ./docker

# Docker push
Write-Host "Pushing Docker image..."
docker push $imageName

# -------------------------------------------- #

# Docker push
Write-Host "Starting Kubernetes..."

Write-Host "Creating Azure Resource Group..."
$resourceGroupOutput = az group create --name scc2324-cluster-$studentNumber --location westeurope

$resourceGroupId = ($resourceGroupOutput | ConvertFrom-Json).id

Write-Host "Creating Service... resourceGroupId = $resourceGroupId"
$serviceOutput = az ad sp create-for-rbac --name http://scc2324-kuber-$studentNumber --role Contributor --scope $resourceGroupId

$serviceJSON = $serviceOutput | ConvertFrom-Json
$appId = $serviceJSON.appId
$password = $serviceJSON.password

Write-Host "Creating Cluster..."
Write-Host "service appId = $appId, password = $password"
$resourceGroupOutput = az aks create --resource-group scc2324-cluster-$studentNumber --name my-scc2324-cluster-$studentNumber --node-vm-size Standard_B2s --generate-ssh-keys --node-count 2 --service-principal $appId  --client-secret $password

Write-Host "Getting Kubernetes credentials..."
az aks get-credentials --resource-group scc2324-cluster-$studentNumber --name my-scc2324-cluster-$studentNumber

Write-Host "Applying yaml in docker directory..."
kubectl apply -f .\docker\sccapp.yaml

