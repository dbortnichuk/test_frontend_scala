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
  val simpleDataPath = envOrNone("BACKEND_SIMPLE_DATA_PATH")

  val SegmentBackendApiVersion = "v1"

  val backend = new Backend(address, port, version, simpleDataPath, )
  val backendBaseUrl = s"http://$address:$port"

  def main(args: Array[String]): Unit = {

    val route =
      path(SegmentBackendApiVersion) {
        get {
          extractRequest { request =>
            parameters(ParamDataSource.?) { dataSourceOption =>
              val responseFuture = backend.getResponse(request, dataSourceOption).map(response =>
                HttpEntity(ContentTypes.`application/json`, response.toJson.toString))
              complete(responseFuture)
            }
          }
        }
      } ~
        path(SegmentBackendApiVersion / SegmentProtected) {
          get {
            extractRequest { request =>
                parameters(ParamDataSource.?, ParamApiKey.as[String]) { (dataSourceOption, apiKey) =>
                val responseFuture = backend.getResponseProtected(request, dataSourceOption, apiKey).map(response =>
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, response.toJson.toString)))
                  .recover {
                    case ae: ApiException =>
                      HttpResponse(ae.status, entity = HttpEntity(ContentTypes.`application/json`, ae.toJson.toString()))
                    case t: Throwable =>
                      HttpResponse(StatusCodes.InternalServerError,
                        entity = HttpEntity(ContentTypes.`application/json`,
                          ApiException(StatusCodes.InternalServerError.intValue, t.getMessage, applicationName).toJson.toString()))
                  }
                complete(responseFuture)
              }
            }
          }
        } ~
        path(SegmentReady) {
          get {
            extractRequest { request =>
              logger.info(s"${request.uri.toString()} -> /$SegmentHealth")
              complete(Http().singleRequest(HttpRequest(uri = Uri(Utils.buildURLQuery(backendBaseUrl, Seq(SegmentHealth))))).map(_.status.value))
            }
          }
        } ~
        path(SegmentHealth) {
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
        logger.info(s"Server version $version now online. Please navigate to http://$interface:$port/$SegmentBackendApiVersion\nPress Ctrl-C to stop...")
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
