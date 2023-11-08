# Group Members

Rafael Mira - 59243 - rs.mira@campus.fct.unl.pt
<br>
Rafael Pereira - 60700 - rl.pereira@campus.fct.unl.pt
<br>
Rafael Martins - 60602 - rlo.martins@campus.fct.unl.pt

# How to execute

-------------------------------------------------------------------------------------
-

1 - <b>Primeiro mudar o número dos ficheiros AzureManagement, pom.xml, AzureFunctions, CognitiveSearch</b>

<b>2 - </b> compile-azure.bat

<b>3 - </b> create-resources.bat

<b>4 - </b> Para cada região: westeurope e centralus (sao as que usamos)

.... 4.1 - <b>No pom.xml, nas tags: region, resourceGroup e appName </b> colocar westeurope ou centralus

.... <b>4.2 - </b>  appDeploy.bat & deployFunctions.bat

.... <b>4.3 - </b>  azureprops-regiao.bat

.... <b>4.4 - </b>  appDeploy.bat (outra vez) (não tenho a certeza se é necessário mas ás vezes falha se não fizer de novo)


-------------------------------------------------------------------------------------
-



