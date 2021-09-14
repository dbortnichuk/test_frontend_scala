#!/bin/bash -ex
set -o pipefail

#sbt clean assembly

docker build --file ./docker/v1/DockerfileBackend -t dbortnichuk/backend_scala:latest .
docker build --file ./docker/v1/DockerfileFrontend -t dbortnichuk/frontend_scala:latest .
docker build --file ./k8s/v1/mysql/Dockerfile -t dbortnichuk/db-mysql-test:8.0.26 .

docker login

docker push dbortnichuk/backend_scala:latest
docker push dbortnichuk/frontend_scala:latest
docker push dbortnichuk/db-mysql-test:8.0.26

# cleanup
docker rmi $(docker images | grep "<none>"|awk '$2=="<none>" {print $3}') --force

docker images