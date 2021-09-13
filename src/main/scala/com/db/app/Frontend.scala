package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import com.db.app.LauncherFrontend.{applicationName, entityToBytes}
import com.db.app.Models.{ApiException, BackendResponse, FrontendResponse, Param}
import com.typesafe.scalalogging.StrictLogging
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Frontend(
                address: String,
                port: String,
                version: String,
                mysqlHost: String,
                mysqlPort: String
              ) extends JsonSupport with Logging {

  def getResponse(
                   request: HttpRequest,
                   backendUri: String,
                   path: Seq[String],
                   params: Seq[Param]): Future[Either[ApiException, FrontendResponse]] = {
    val backandUriQuery = Utils.buildURLQuery(backendUri, path, params)
    logger.info(s"${request.uri.toString()} -> $backandUriQuery")

    val backendHttpResponseFuture = Http().singleRequest(HttpRequest(uri = Uri(backandUriQuery)))
    val backendResponseFuture = backendHttpResponseFuture.flatMap{response =>
      entityToBytes(response.entity).map{bytes =>
        val jsonObj = new String(bytes).parseJson
        Try(jsonObj.convertTo[BackendResponse]) match {
          case Success(value) => Right(value)
          case Failure(exception) => {
            logger.error(exception.getMessage)
            Left(jsonObj.convertTo[ApiException])
          }
        }

      }
    }
    val frontendResponseFuture: Future[Either[ApiException, FrontendResponse]] = backendResponseFuture.map{
        case Right(backendResponse) => Right(FrontendResponse(applicationName, com.db.app.language, version, address, port, Some(backendResponse)))
        case Left(apiException) => Left(apiException)
      }
    frontendResponseFuture
  }

  val dataSource = new MysqlDAL(mysqlHost, mysqlPort)
  def getResponseDirect(request: HttpRequest): Future[Either[ApiException, FrontendResponse]] = {
    logger.info(s"${request.uri.toString()} -> ${dataSource.uri}")

    val responseFuture = withErrorLogging(dataSource.get().map(data =>
      Right(FrontendResponse(applicationName, com.db.app.language, version, address, port,
        Some(BackendResponse("mysql", "", "", mysqlHost, mysqlPort, data)))))
    )
      .recover(t => Left(ApiException(StatusCodes.InternalServerError.intValue, t.getMessage, s"${getClass.getName}")))
    responseFuture
  }


}
