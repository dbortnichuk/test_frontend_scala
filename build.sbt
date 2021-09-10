
scalaVersion := "2.13.6"
lazy val `test_scala_apps` = project in file(".")

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.6"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "ch.qos.logback"                %  "logback-classic"        % "1.2.3",
  "com.typesafe.scala-logging"    %% "scala-logging"          % "3.9.2",
  "commons-io"                    % "commons-io"              % "2.7",
  "mysql"                         % "mysql-connector-java"    % "8.0.26"
)