package com.db.app

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import com.db.app.LauncherFrontend.{applicationName, entityToBytes}
import com.db.app.Models.{ApiException, BackendResponse, FrontendResponse}
import spray.json._

import scala.concurrent.Future

class Frontend(address: String, port: String, version: String) extends JsonSupport{

  def getResponse(backendEndpoint: String): Future[Either[ApiException, FrontendResponse]] = {
    val backendHttpResponseFuture = Http().singleRequest(HttpRequest(uri = Uri(backendEndpoint)))
    val backendResponseFuture = backendHttpResponseFuture.flatMap(response =>
      entityToBytes(response.entity).map(bytes => new String(bytes).parseJson.convertTo[BackendResponse]))
    val frontendResponseFuture = backendResponseFuture.map(backendResponse =>
      FrontendResponse(applicationName, com.db.app.language, version, address, port, Some(backendResponse)))
    frontendResponseFuture
  }


}
