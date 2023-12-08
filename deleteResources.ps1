$studentNumber = Read-Host "Enter student number"

$propertiesContent = (Get-Content -Path ".\config.properties" -Raw)

if (-not ($propertiesContent -match "^$studentNumber=")) {
    Write-Host "StudentNumber '$studentNumber' does not exist"
    Exit 1
}

kubectl delete deployments,services,pods --all

#kubectl delete pv --all

Write-Host "Deleting Azure resource group..."
az group delete --resource-group scc2324-cluster-$studentNumber --yes

Write-Host "Check Azure resources and delete leftovers"