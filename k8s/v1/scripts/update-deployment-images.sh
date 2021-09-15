#!/bin/bash -ex
set -o pipefail
shopt -s expand_aliases

source "$HOME/scripts/kubectl-shortcuts.sh"
source "$HOME/scripts/eksctl-shortcuts.sh"

k8s/v1/scripts/update-images.sh

k rollout restart deployment/frontend-scala
k rollout restart deployment/backend-scala
k rollout restart deployment/mysql

k get pods -o wide
minikube service backend-scala-nodeport --url
minikube service frontend-scala-nodeport --url
minikube service mysql-nodeport --url