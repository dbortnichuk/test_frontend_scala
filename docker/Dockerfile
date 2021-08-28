FROM openjdk:11

RUN mkdir /opt/test_frontend_scala
COPY ./target/scala-2.13/test_frontend_scala-assembly-0.1.0-SNAPSHOT.jar /opt/test_frontend_scala/test_frontend_scala-assembly-0.1.0-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java", "-cp", "/opt/test_frontend_scala/test_frontend_scala-assembly-0.1.0-SNAPSHOT.jar", "com.db.app.Launcher"]
