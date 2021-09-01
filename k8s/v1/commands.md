
[BASIC]
k create -f k8s/v1/pod-backend.yaml
k port-forward pod/backend-scala 9090:9090  # local port : pod port

k create -f k8s/v1/pod-frontend.yaml
k port-forward pod/frontend-scala 9100:9100  # local port : pod port

k exec --stdin --tty frontend-scala -- /bin/bash

[LOCAL DEPLOYMENT]
# Deploy and connect

k create -f k8s/v1/deployment-backend.yaml
// k expose deployment backend-scala-nodeport --type=NodePort --port 9090 // will be available at 30xxx port
k create -f k8s/v1/service-backend-nodeport.yaml
minikube service backend-scala-nodeport --url

k create -f k8s/v1/deployment-frontend.yaml
// k expose deployment frontend-scala-nodeport --type=NodePort --port 9100 // will be available at 30xxx port
k create -f k8s/v1/service-frontend-nodeport.yaml
minikube service frontend-scala-nodeport --url
k create -f k8s/v1/service-backend-clusterip.yaml

k get all -o wide

# Update/Undo containers
k rollout history deployment/frontend-scala
k rollout history deployment/frontend-scala --revision=1 # details
k rollout status deployment/frontend-scala

k set image deployment/frontend-scala frontend-scala=dbortnichuk/frontend_scala:deprecated --record
k scale deployment frontend-scala --replicas 1 --record
k apply -f k8s/v1/deployment-frontend-deprecated.yaml --record  # change image and scale

k rollout undo deployment/frontend-scala --to-revision=1
k rollout restart deployment/frontend-scala # redeploy with updated image of the same version, useful when working with :latest

# Autoscale
k create -f k8s/v1/hpa-frontend-scala.yaml
