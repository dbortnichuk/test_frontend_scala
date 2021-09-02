package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import com.db.app.Models.ApiException
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import java.net.InetAddress
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Properties.{envOrElse, envOrNone}
import scala.util.{Failure, Success}

object LauncherBackend extends JsonSupport with StrictLogging {

  val applicationName = "backend-app"

  val version = envOrElse("BACKEND_APP_VERSION", latestVersion)
  val address = InetAddress.getLocalHost.getHostAddress
  val port = envOrElse("BACKEND_PORT", "9090")
  val dataPath = envOrNone("BACKEND_SIMPLE_DATA_PATH")

  val backend = new Backend(address, port, version, dataPath)
  val backendEndpointHealth = s"http://$address:$port/health"

  def main(args: Array[String]): Unit = {

    val endpoint = "v1"
    val route =
      path(endpoint) {
        get {
          extractRequest { request =>
            logger.info(request.uri.toString())
            val responseFuture = backend.getResponse().map(response =>
              HttpEntity(ContentTypes.`application/json`, response.toJson.toString))
            complete(responseFuture)
          }
        }
      } ~
        path(endpoint / "protected") {
          get {
            parameters("apiKey".as[String]) { apiKey =>
              extractRequest { request =>
                logger.info(request.uri.toString())
                val responseFuture = backend.getResponseProtected(apiKey).map(response =>
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, response.toJson.toString)))
                  .recoverWith {
                    case ae: ApiException =>
                      HttpResponse(ae.status, entity = HttpEntity(ContentTypes.`application/json`, ae.toJson.toString()))
                    case t: Throwable => HttpResponse(StatusCodes.InternalServerError, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, t.getMessage))
                  }
                complete(responseFuture)
              }
            }
          }
        } ~
        path("ready") {
          get {
            extractRequest { request =>
              logger.info(s"${request.uri.toString()} -> /health")
              complete(Http().singleRequest(HttpRequest(uri = Uri(backendEndpointHealth))).map(_.status.value))
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
