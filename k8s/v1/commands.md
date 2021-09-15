
[BASIC]
k create -f k8s/v1/pod-backend.yaml
k port-forward pod/backend-scala 9090:9090  # local port : pod port

k create -f k8s/v1/pod-frontend.yaml
k port-forward pod/frontend-scala 9100:9100  # local port : pod port

k exec --stdin --tty frontend-scala -- /bin/bash

[LOCAL DEPLOYMENT]
# Secret
# Will enable access via /v1/protected endpoint
echo -n "key123" | base64 # a2V5MTIz
echo -n "a2V5MTIz" | base64 --decode # key123

k create -f k8s/v1/secret-shared.yaml
k get secret shared -o yaml # view data, describe does not show

# Deploy and connect

k create -f k8s/v1/configmap-shared.yaml

k create -f k8s/v1/deployment-backend.yaml
// k expose deployment backend-scala-nodeport --type=NodePort --port 9090 // will be available at 30xxx port
k create -f k8s/v1/service-backend-nodeport.yaml
k create -f k8s/v1/service-backend-clusterip.yaml
minikube service backend-scala-nodeport --url

k create -f k8s/v1/deployment-frontend.yaml
// k expose deployment frontend-scala-nodeport --type=NodePort --port 9100 // will be available at 30xxx port
k create -f k8s/v1/service-frontend-nodeport.yaml
k create -f k8s/v1/service-frontend-clusterip.yaml // for ingress
minikube service frontend-scala-nodeport --url


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
# local
minikube addons enable metrics-server
minikube addons list
k top node

# AWS
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl get deployment metrics-server -n kube-system

k create -f k8s/v1/hpa-frontend.yaml //  for autoscaling to work enable metrics-server and make sure 'resources' are declared for pods in deployment


# Ingress
k create -f k8s/v1/ingress-host.yaml
k create -f k8s/v1/ingress-path.yaml

---Local---
minikube addons enable ingress
kubectl get all -n ingress-nginx // check if ingress controller is running

// add to /etc/hosts local dns entries, smth like
192.168.49.2    frontend.bortnichuk.com backend.bortnichuk.com protected.bortnichuk.com
192.168.49.2    www.bortnichuk.com

---AWS---
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.0.0/deploy/static/provider/aws/deploy.yaml
kubectl get all -n ingress-nginx // check if ingress controller is running

// create A records in AWS pointing to loadbalancer created by nginx ingress
// deploy your app objects eg. secret, configmap, deployments, hpa, clusterip

k get ingress // check deployed ingresses hosts/ports
// for both local and AWS app will be available like:
http://www.bortnichuk.com/frontend
http://www.bortnichuk.com/backend
http://frontend.bortnichuk.com/v1
http://backend.bortnichuk.com/v1
http://protected.bortnichuk.com/v1/protected

[mysql]
k create -f k8s/v1/pv-mysql.yaml
k create -f k8s/v1/pvc-mysql.yaml
k create -f k8s/v1/deployment-mysql.yaml
k create -f k8s/v1/service-mysql-clusterip.yaml
k create -f k8s/v1/service-mysql-nodeport.yaml
minikube service db-mysql-nodeport --url

[network policy]



k create -f k8s/v1/netpol-mysql.yaml // make mysql available only from backend pods

[helm]
helm install infra1 ./k8s/v1/helm
helm list
helm uninstall infra1

helm install infra2 ./k8s/v1/helm --set frontend.replicas.min=2
helm upgrade infra2 ./k8s/v1/helm --set frontend.replicas.min=1
helm upgrade infra2 ./k8s/v1/helm -f prd-values.yaml # supply custom values file here

helm package ./k8s/v1/helm/
helm upgrade infra2 ./helm-v1-0.1.0.tgz

helm list
helm uninstall infra2







