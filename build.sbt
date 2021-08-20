
scalaVersion := "2.13.6"
lazy val `test_frontend_scala` = project in file(".")

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.6"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
)