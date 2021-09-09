package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.util.ByteString
import com.db.app.Models.{FrontendResponse, Param}
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import java.net.InetAddress
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Properties.{envOrElse, envOrNone}
import scala.util.{Failure, Success}


object LauncherFrontend extends JsonSupport with StrictLogging {

  val applicationName = "frontend-app"

  val version = envOrElse("FRONTEND_APP_VERSION", latestVersion)
  //val version = "deprecated"
  val address = InetAddress.getLocalHost.getHostAddress
  val port = envOrElse("FRONTEND_PORT", "9100")
  val backendHost = envOrElse("BACKEND_TARGET_HOST", "0.0.0.0")
  val backendPort = envOrElse("BACKEND_TARGET_PORT", "9090")
  val backendApiKey = envOrElse("BACKEND_API_KEY", "")

  val backendBaseUrl = s"http://$backendHost:$backendPort"
  val SegmentFrontendApiVersion = "v1"

  val frontend = new Frontend(address, port, version)

  def main(args: Array[String]): Unit = {

    val route =
      path(SegmentFrontendApiVersion) {
        get {
          extractRequest { request =>
            parameters(ParamDataSource.?) { dataSourceOption =>
              val params = dataSourceOption.toSeq.map(sourceVal => Param(ParamDataSource, sourceVal))
              val entityFuture = frontend.getResponse(request, backendBaseUrl, Seq(LauncherBackend.SegmentBackendApiVersion), params).map(frontendResponse =>
                HttpEntity(ContentTypes.`application/json`, frontendResponse.toJson.toString()))

              complete(entityFuture)
            }
          }
        }
      } ~
        path(SegmentFrontendApiVersion / SegmentDirect) {
          get {
            extractRequest { request =>
              parameters(ParamDataSource.?) { sourceOption =>
                val params = sourceOption.toSeq.map(sourceVal => Param(ParamDataSource, sourceVal))
                val entityFuture = frontend.getResponseDirect(request, "database, s3 or other ds uri", Seq("required path stub"), params)
                  .map(frontendResponse => HttpEntity(ContentTypes.`application/json`, frontendResponse.toJson.toString()))

                complete(entityFuture)
              }
            }
          }
        } ~
        path(SegmentFrontendApiVersion / SegmentProtected) {
          get {
            extractRequest { request =>
              val entityFuture = frontend.getResponse(
                request,
                backendBaseUrl,
                Seq(SegmentFrontendApiVersion, SegmentProtected),
                Seq(Param(ParamApiKey, backendApiKey)))
                .map {
                  case Right(response) => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, response.toJson.toString()))
                  case Left(apiException) =>
                    HttpResponse(apiException.status, entity = HttpEntity(ContentTypes.`application/json`, apiException.toJson.toString()))
                }
              complete(entityFuture)
            }
          }
        } ~
        path(SegmentFrontendApiVersion / SegmentLocal) {
          get {
            extractRequest { request =>
              logger.info(request.uri.toString())

              val entityFuture = HttpEntity(ContentTypes.`application/json`,
                FrontendResponse(applicationName, com.db.app.language, version, address, port, None).toJson.toString())
              complete(entityFuture)
            }
          }
        } ~
        path(SegmentReady) {
          extractRequest { request =>
            val backendUrl = Utils.buildURLQuery(backendBaseUrl, Seq(LauncherBackend.SegmentBackendApiVersion))
            logger.info(s"${request.uri.toString()} -> $backendUrl")
            complete(Http().singleRequest(HttpRequest(uri = Uri(backendUrl))).map(_.status.value))
          }
        } ~
        path(SegmentHealth) {
          extractRequest { request =>
            logger.info(request.uri.toString())

            complete(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "OK")))
          }
        }
    val interface = "0.0.0.0"
    val bindingFuture = Http(system).bindAndHandle(route, interface, port.toInt)

    bindingFuture.onComplete {
      case Success(b) ⇒
        println(s"Server version $version now online. Please navigate to http://$interface:$port/$SegmentFrontendApiVersion\nPress Ctrl-C to stop...")
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
