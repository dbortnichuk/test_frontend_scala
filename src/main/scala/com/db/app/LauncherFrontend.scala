package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.Materializer
import akka.util.ByteString
import com.db.app.Models.{ApiException, FrontendResponse, Param}
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import java.net.InetAddress
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Properties.{envOrElse, envOrNone}
import scala.util.{Failure, Success}
import fr.davit.akka.http.metrics.core.{HttpMetricsRegistry, HttpMetricsSettings}
import fr.davit.akka.http.metrics.core.HttpMetrics._
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.akka.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import io.prometheus.client.CollectorRegistry


object LauncherFrontend extends JsonSupport with StrictLogging {

  val applicationName = "frontend-app"

  val version = envOrElse("FRONTEND_APP_VERSION", latestVersion)
  //val version = "deprecated"
  val address = InetAddress.getLocalHost.getHostAddress
  val port = envOrElse("FRONTEND_PORT", "9100")
  val backendHost = envOrElse("BACKEND_TARGET_HOST", "0.0.0.0")
  val backendPort = envOrElse("BACKEND_TARGET_PORT", "9090")
  val backendApiKey = envOrElse("BACKEND_API_KEY", "")
  val mysqlHost = envOrElse("DB_MYSQL_HOST", "0.0.0.0")
  val mysqlPort = envOrElse("DB_MYSQL_PORT", "3306")
  //    val mysqlHost = envOrElse("DB_MYSQL_HOST", "192.168.49.2")
  //    val mysqlPort = envOrElse("DB_MYSQL_PORT", "30306")

  val backendBaseUrl = s"http://$backendHost:$backendPort"
  val SegmentFrontendApiVersion = "v1"

  val frontend = new Frontend(address, port, version, mysqlHost, mysqlPort)

  def main(args: Array[String]): Unit = {
    val prometheusRegistry = PrometheusRegistry(new CollectorRegistry(), PrometheusSettings.default)
    val route =
      path(SegmentFrontendApiVersion) {
        get {
          handleExceptions(LauncherBackend.exceptionHandler(applicationName)) {
            extractRequest { request =>
              parameters(ParamDataSource.?) { dataSourceOption =>
                val params = dataSourceOption.toSeq.map(sourceVal => Param(ParamDataSource, sourceVal))
                val entityFuture = frontend.getResponse(request, backendBaseUrl, Seq(LauncherBackend.SegmentBackendApiVersion), params)                  .map {
                  case Right(response) => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, response.toJson.toString()))
                  case Left(apiException) =>
                    HttpResponse(apiException.status, entity = HttpEntity(ContentTypes.`application/json`, apiException.toJson.toString()))
                }
                complete(entityFuture)
              }
            }
          }
        }
      } ~
        path(SegmentFrontendApiVersion / SegmentDirect) {
          get {
            handleExceptions(LauncherBackend.exceptionHandler(applicationName)) {
              extractRequest { request =>
                parameters(ParamDataSource.?) { sourceOption =>
                  val entityFuture = frontend.getResponseDirect(request)
                    .map {
                      case Right(response) => HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, response.toJson.toString()))
                      case Left(apiException) =>
                        HttpResponse(apiException.status, entity = HttpEntity(ContentTypes.`application/json`, apiException.toJson.toString()))
                    }

                  complete(entityFuture)
                }
              }
            }
          }
        } ~
        path(SegmentFrontendApiVersion / SegmentProtected) {
          get {
            handleExceptions(LauncherBackend.exceptionHandler(applicationName)) {
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
          }
        } ~
        path(SegmentFrontendApiVersion / SegmentLocal) {
          get {
            handleExceptions(LauncherBackend.exceptionHandler(applicationName)) {
              extractRequest { request =>
                logger.info(request.uri.toString())

                val entityFuture = HttpEntity(ContentTypes.`application/json`,
                  FrontendResponse(applicationName, com.db.app.language, version, address, port, None).toJson.toString())
                complete(entityFuture)
              }
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
        } ~
        path(SegmentMetrics) {
          get {
            extractRequest { request =>
              logger.info(request.uri.toString())
              metrics(prometheusRegistry)
            }
          }
        }

    val interface = "0.0.0.0"
    val bindingFuture = Http().newMeteredServerAt(interface, port.toInt, prometheusRegistry).bindFlow(route)

//    val bindingFuture = Http(system).bindAndHandle(route, interface, port.toInt)

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
