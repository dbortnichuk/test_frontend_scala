package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.db.app.Models.BackendModel
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import java.net.InetAddress
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Properties.envOrElse
import scala.util.{Failure, Success}

object LauncherBackend extends JsonSupport with StrictLogging {

  val applicationName = "backend-app"

  val version = envOrElse("BACKEND_APP_VERSION", latestVersion)
  val address = InetAddress.getLocalHost.getHostAddress
  val port = envOrElse("BACKEND_PORT", "9090")

  def main(args: Array[String]): Unit = {

    val endpoint = "v1"
    val route =
      path(endpoint) {
        get {
          extractRequest { request =>
            logger.info(request.uri.toString())
            complete(HttpEntity(ContentTypes.`application/json`,
              BackendModel(applicationName, com.db.app.language, version, address, port).toJson.toString()))
          }
        }
      } ~
        path("health") {
          get {
            extractRequest { request =>
              logger.info(request.uri.toString())
              complete(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK")))
            }
          }
        }

    val interface = "0.0.0.0"
    val bindingFuture = Http(system).bindAndHandle(route, interface, port.toInt)

    bindingFuture.onComplete {
      case Success(b) ⇒
        logger.info(s"Server version $version now online. Please navigate to http://$interface:$port/$endpoint\nPress Ctrl-C to stop...")
        sys.addShutdownHook {
          Await.result(b.unbind(), 30.seconds)
          system.terminate()
        }
      case Failure(_) ⇒
        println(s"Failed to bind on port $port")
        sys.addShutdownHook {
          system.terminate()
        }
        sys.exit(1)
    }
  }

}
