package com.db.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.json._

import scala.io.StdIn

case class ResponseModel(application: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val ErrorMessageFormat: RootJsonFormat[ResponseModel] = jsonFormat1(ResponseModel)

}

object Launcher extends JsonSupport {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val route =
      path("hello") {
        get {
          println("received")
          complete(HttpEntity(ContentTypes.`application/json`, ResponseModel("Scala_Frontent").toJson.toString()))
        }
      }

    val interface = "0.0.0.0"
    val port = 8080
    val bindingFuture = Http().newServerAt(interface, port).bind(route)

    println(s"Server now online. Please navigate to http://$interface:$port/hello\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
