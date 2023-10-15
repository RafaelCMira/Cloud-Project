@echo off
::az functionapp config appsettings set --name scc24functionswesteurope59243 --resource-group scc24-rg-westeurope-59243 --settings "BlobStoreConnection=DefaultEndpointsProtocol=https;AccountName=scc24stwesteurope59243;AccountKey=hxQrYvMJpFe9VepO6uNwJglP0snqkQPJHOBiyroioHSlxdOjEhRio+g3ZXzQMrrv402DcVJwSI2h+ASt3TfNzg==;EndpointSuffix=core.windows.net"

az functionapp config appsettings set --name scc24app-westeurope-59243 --resource-group scc24-rg-westeurope-59243 --settings "BlobStoreConnection=DefaultEndpointsProtocol=https;AccountName=scc24stwesteurope59243;AccountKey=hxQrYvMJpFe9VepO6uNwJglP0snqkQPJHOBiyroioHSlxdOjEhRio+g3ZXzQMrrv402DcVJwSI2h+ASt3TfNzg==;EndpointSuffix=core.windows.net"
az functionapp config appsettings set --name scc24app-westeurope-59243 --resource-group scc24-rg-westeurope-59243 --settings "COSMOSDB_KEY=5V69jAi5WFxutO9pymG1tVY6NoJ7iNB946vbkRkNdNyDQgesaqkO9bytDkaE5nMnNS78ZYq8dJygACDbNOk4og=="
az functionapp config appsettings set --name scc24app-westeurope-59243 --resource-group scc24-rg-westeurope-59243 --settings "COSMOSDB_URL=https://scc24account59243.documents.azure.com:443/"
az functionapp config appsettings set --name scc24app-westeurope-59243 --resource-group scc24-rg-westeurope-59243 --settings "COSMOSDB_DATABASE=scc24db59243"

:: az functionapp config appsettings set --name scc24functionswesteurope59243 --resource-group scc24-rg-westeurope-59243 --settings "AzureCosmosDBConnection=AccountEndpoint=https://scc24account59243.documents.azure.com:443/;AccountKey=5V69jAi5WFxutO9pymG1tVY6NoJ7iNB946vbkRkNdNyDQgesaqkO9bytDkaE5nMnNS78ZYq8dJygACDbNOk4og==;"

