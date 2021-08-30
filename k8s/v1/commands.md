
[BASIC]
k create -f k8s/v1/pod-backend.yaml
k port-forward pod/backend-scala 9090:9090  # local port : pod port

k create -f k8s/v1/pod-frontend.yaml
k port-forward pod/frontend-scala 9100:9100  # local port : pod port

[LOCAL DEPLOYMENT]
k create -f k8s/v1/deployment-backend.yaml
// k expose deployment backend-scala-nodeport --type=NodePort --port 9090 // will be available at 30xxx port
k create -f k8s/v1/service-backend-nodeport.yaml
minikube service backend-scala-nodeport --url



k create -f k8s/v1/deployment-frontend.yaml
// k expose deployment frontend-scala-nodeport --type=NodePort --port 9100 // will be available at 30xxx port
k create -f k8s/v1/service-frontend-nodeport.yaml
minikube service frontend-scala-nodeport --url

k get all -o wide
