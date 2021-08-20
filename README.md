
[ENV]
java -cp target/scala-2.13/test_frontend_scala-assembly-0.1.0-SNAPSHOT.jar com.db.app.Launcher

curl -X GET  http://localhost:8080/hello

[DOCKER]
docker build -t dbortnichuk/test_frontend_scala:2 .
docker build --file ./Dockerfile -t dbortnichuk/test_frontend_scala:2 .

docker tag  test_frontend_scala:1 dbortnichuk/test_frontend_scala:1

docker login

docker push dbortnichuk/test_frontend_scala:tagname

docker run -it --rm -p 8080:8080 dbortnichuk/test_frontend_scala:2

docker rmi <id> -f

[k8s]
kubectl run frontend --image=dbortnichuk/test_frontend_scala:2 --port=8080

kubectl --v=8 logs frontend-scala
