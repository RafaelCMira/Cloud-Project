@echo off
set /P confirm=Mudas-te o numero do AzureManagement, pom.xml, Azure Functions? (Y/N)
if /i "%confirm%"=="Y" (
     java -cp target/scc2324-1.0-jar-with-dependencies.jar scc.utils.mgt.AzureManagement
) else (
    echo Faz compile de novo quando mudares o numero
)
