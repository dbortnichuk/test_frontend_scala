# Contact api server
minikube ip
k config view
APISERVER=$(kubectl config view -o jsonpath="{.clusters[?(@.name==\"$CLUSTER_NAME\")].cluster.server}")
TOKEN=$(kubectl get secrets -o jsonpath="{.items[?(@.metadata.annotations['kubernetes\.io/service-account\.name']=='default')].data.token}"|base64 --decode)
curl -X GET --cacert ~/.minikube/ca.crt --header "Authorization: Bearer $TOKEN" $APISERVER/version

# Adding new user
mkdir creds && cd creds
openssl genrsa -out newuser.key 2048
openssl req -new -key newuser.key -out newuser.csr -subj "/CN=newuser/O=org1"\n