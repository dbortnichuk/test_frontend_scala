package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.util.ByteString
import com.db.app.Models.{BackendModel, FrontendModel}
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import java.net.InetAddress
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Properties.envOrElse
import scala.util.{Failure, Success}


object LauncherFrontend extends JsonSupport with StrictLogging {

  val applicationName = "frontend-app"

  val version = envOrElse("FRONTEND_APP_VERSION", latestVersion)
  val address = InetAddress.getLocalHost.getHostAddress
  val port = envOrElse("FRONTEND_PORT", "9100")
  val backendHost = envOrElse("BACKEND_TARGET_HOST", "0.0.0.0")
  val backendPort = envOrElse("BACKEND_TARGET_PORT", "9090")

  val endpoint = "v1"
  val backendEndpoint = s"http://$backendHost:$backendPort/$endpoint"

  def main(args: Array[String]): Unit = {

    val route =
      path(endpoint) {
        get {
          extractRequest { request =>
            logger.info(s"${request.uri.toString()} -> $backendEndpoint")
            val backendResponseFuture = Http().singleRequest(HttpRequest(uri = Uri(backendEndpoint)))

            val backendModelFuture: Future[BackendModel] = backendResponseFuture.flatMap(response =>
              entityToBytes(response.entity).map(bytes => new String(bytes).parseJson.convertTo[BackendModel]))

            val entityFuture = backendModelFuture.map(backendModel => HttpEntity(ContentTypes.`application/json`,
              FrontendModel(applicationName, com.db.app.language, version, address, port, Some(backendModel)).toJson.toString()))

            complete(entityFuture)
          }
        }
      } ~
      path(endpoint / "local") {
        get {
          extractRequest { request =>
            logger.info(request.uri.toString())
            val entityFuture = HttpEntity(ContentTypes.`application/json`,
              FrontendModel(applicationName, com.db.app.language, version, address, port, None).toJson.toString())
            complete(entityFuture)
          }
        }
      } ~
      path("ready") {
        extractRequest { request =>
          logger.info(s"${request.uri.toString()} -> $backendEndpoint")
          complete(Http().singleRequest(HttpRequest(uri = Uri(backendEndpoint))).map(_.status.value))
        }
      } ~
      path("health") {
        extractRequest { request =>
          logger.info(request.uri.toString())
          complete(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK")))
        }
      }
    val interface = "0.0.0.0"
    val bindingFuture = Http(system).bindAndHandle(route, interface, port.toInt)

    bindingFuture.onComplete {
      case Success(b) ⇒
        println(s"Server version $version now online. Please navigate to http://$interface:$port/$endpoint\nPress Ctrl-C to stop...")
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

  def entityToBytes(entity: HttpEntity)(implicit executionContext: ExecutionContext, materializer: Materializer): Future[Array[Byte]] = {
    entity.dataBytes.runFold(ByteString.empty) { case (acc, b) => acc ++ b }.map(_.toArray)
  }

}
