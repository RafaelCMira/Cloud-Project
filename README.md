# Group Members

Rafael Mira - 59243 - rs.mira@campus.fct.unl.pt
<br>
Rafael Pereira - 60700 - rl.pereira@campus.fct.unl.pt
<br>
Rafael Martins - 60602 - rlo.martins@campus.fct.unl.pt

---

# How to deploy using docker

Use the following command to compile, build docker image and push to docker hub.

```shell
./buildDocker.bat
```

To run locally you can use

```shell
docker run --rm -p 8080:8080 <yourImagename>
```

After this you can check your image in the URL:

```
http://localhost:8080/name_of_war_file
```

---

# How to execute (para testar cog search usar apenas 1 região e não dar deploy das az functions)

-------------------------------------------------------------------------------------
-

1 - <b>Primeiro mudar o número dos ficheiros AzureManagement, pom.xml, AzureFunctions, CognitiveSearch</b>

<b>2 - </b> compile-azure.bat

<b>3 - </b> create-resources.bat

<b>4 - </b> Para cada região: westeurope e centralus (sao as que usamos)

.... 4.1 - <b>No pom.xml, na tag: region </b> colocar westeurope ou centralus

.... <b>4.2 - </b>  appDeploy.bat & deployFunctions.bat

.... <b>4.3 - </b>  azureprops-regiao.bat

.... <b>4.4 - </b> 1º compile-azure.bat && 2º search-azureprops-regiao.bat

.... <b>4.5 - </b>  appDeploy.bat


-------------------------------------------------------------------------------------
-



