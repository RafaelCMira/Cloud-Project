$studentNumber = Read-Host "Enter student number"

$propertiesContent = (Get-Content -Path ".\config.properties" -Raw)

if (-not ($propertiesContent -match "^$studentNumber=")) {
    Write-Host "StudentNumber '$studentNumber' does not exist"
    Exit 1
}

$imageName = ($propertiesContent | ConvertFrom-StringData)[$studentNumber]

kubectl delete deployments,services,pods --all

Write-Host "Executing Maven commands..."
mvn clean compile package

Write-Host "Logging into Docker..."
docker login

Write-Host "Building Docker image..."
docker build -t $imageName ./docker

Write-Host "Pushing Docker image..."
docker push $imageName

Write-Host "Applying yaml in docker directory..."
kubectl apply -f .\docker\sccapp.yaml