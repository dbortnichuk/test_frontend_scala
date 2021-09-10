#Useful commands
// commands should be run from the root of the project

[ENV]
java -cp target/scala-2.13/test_frontend_scala-assembly-0.1.0-SNAPSHOT.jar com.db.app.Launcher

curl -X GET  http://localhost:9090/v1

[DOCKER]
docker build -t dbortnichuk/test_frontend_scala:2 .
docker build --file ./docker/v1/DockerfileBackend -t dbortnichuk/backend_scala:latest .

docker tag  test_frontend_scala:1 dbortnichuk/backend_scala:latest

docker login

docker push dbortnichuk/backend_scala:latest

docker run -it --rm -p 9090:9090 dbortnichuk/backend_scala:latest

docker rmi <id> -f

[k8s]
--General--
kubectl get componentstatuses
kubectl cluster-info
kubectl get nodes

kubectl config get-contexts
kubectl config current-context
kubectl config use-context my-context

--Local--
minikube start
minikube start --cpus=2 --memory=3gb --disk-size=25gb
minikube stop
minikube delete
minikube ssh
minikube service <name> --url

minikube addons enable metrics-server
minikube addons list

kubectl top node minikube # doesnt work for minikube

--AWS--
eksctl create cluster --name test1 --nodes 2 --zones us-east-1a,us-east-1b
eksctl delete cluster --name test1


kubectl run frontend --image=dbortnichuk/test_frontend_scala:8 --port=8080

kubectl --v=8 logs frontend-scala
kubectl get events
kubectl exec --stdin --tty frontend-scala -- /bin/bash
kubectl port-forward pod/frontend-scala 9090:8080  # local port : pod port
k create -f k8s/pod-frontend-v1.yaml
k delete -f k8s/pod-frontend-v1.yaml

k create deployment frontend --image=dbortnichuk/test_frontend_scala:8
k scale deployment frontend --replicas 4
k autoscale deployment frontend --min=2 --max=4 --cpu-percent=80
k rollout history deployment/frontend
k rollout status deployment/frontend
k set image deployment/frontend test-frontend-scala-qc5p4=dbortnichuk/test_frontend_scala:7 --record
k rollout undo deployment/frontend
k rollout undo deployment/frontend --to-revision=2
k rollout restart deployment/frontend # redeploy with updated image of the same version, useful when working with :latest
k apply -f k8s/dep-frontend-v1.yaml # update image
k get hpa


k expose deployment frontend-scala --type=ClusterIP --port 8080
k expose deployment frontend-scala --type=NodePort --port 8080
k expose deployment frontend-scala --type=LoadBalancer --port 8080 # AWS or Google

-----ingress----
k create deployment main --image=adv4000/k8sphp:latest
k create deployment web1 --image=adv4000/k8sphp:version1
k create deployment web2 --image=adv4000/k8sphp:version2
k create deployment webx --image=adv4000/k8sphp:versionx
k create deployment tomcat --image=tomcat:8.5.38

k scale deployment main --replicas 2
k scale deployment web1 --replicas 2
k scale deployment web2 --replicas 2
k scale deployment webx --replicas 2

k expose deployment main --type=ClusterIP --port 80
k expose deployment web1 --type=ClusterIP --port 80
k expose deployment web2 --type=ClusterIP --port 80
k expose deployment webx --type=ClusterIP --port 80
k expose deployment tomcat --type=ClusterIP --port 8080

[helm]
helm list
helm install v1 helm
helm install v1 helm/ --set frontend.image=dbortnichuk/test_frontend_scala:8 --set frontend.replicas=2
helm install v2 helm/ -f helm/values-prd.yaml
helm upgrade v1 helm/ --set frontend.image=dbortnichuk/test_frontend_scala:8 --set frontend.replicas=1
helm package helm/ # Successfully packaged chart and saved it to: /home/dmytro/workspace/test_frontend_scala/helm-auto-example-0.1.0.tgz
helm install v3 helm-auto-example-0.1.0.tgz

helm search repo
helm search hub apache

helm repo add bitnami https://charts.bitnami.com/bitnami
helm install my-apache bitnami/apache

helm search repo bitnami

helm delete my-apache


