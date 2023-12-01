# Group Members

Rafael Mira - 59243 - rs.mira@campus.fct.unl.pt
<br>
Rafael Pereira - 60700 - rl.pereira@campus.fct.unl.pt
<br>
Rafael Martins - 60602 - rlo.martins@campus.fct.unl.pt

---

# Utils

Run this script to do all the commands below at once

```shell
doesItall.ps1
```

To kill Kubernetes resources

```shell
kubectl delete deployments,services,pods --all
```

See Pods

```shell
kubectl get pods
```

See Services

```shell
kubectl get services
```

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
http://localhost:8080/scc2324-1.0
```

---

## Deploying Kubernetes

To run in azure Kubernetes you will need to create the azure resources first:

Create resource group

```shell
 az group create --name scc2324-cluster-<student_number> --location westeurope
```

Create service

```shell
 az ad sp create-for-rbac --name http://scc2324-kuber-<student_number> --role Contributor --scope <result_id_from_command_above> 
```

Create cluster

```shell
az aks create --resource-group scc2324-cluster-<student_number> --name my-scc2324-cluster-<student_number> --node-vm-size Standard_B2s --generate-ssh-keys --node-count 2 --service-principal <appId_from_command_above>  --client-secret <pwd_from_command_above>
 ```

Get credentials to access the kubernetes cluster

```shell
az aks get-credentials --resource-group scc2324-cluster-<student_number> --name my-scc2324-cluster-<student_number>
```

And lastly you just need to apply our yaml in the docker dir.

```shell
kubectl apply -f .\docker\sccapp.yaml
```

---

## TO-DO

- [X] Yaml script to generate Kubernetes containers;
- [ ] Make a script to create the necessary resources (resource group, service ,cluster, get credentials);
- [ ] Change DB to use MongoDB;
- [ ] Change azure functions;

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



