package com.db.app

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.db.app.Models.{ApiException, BackendResponse, FrontendResponse}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val BackendModelFormat: RootJsonFormat[BackendResponse] = jsonFormat6(BackendResponse)
  implicit val FrontendModelFormat: RootJsonFormat[FrontendResponse] = jsonFormat6(FrontendResponse)
  implicit val ApiFormat: RootJsonFormat[ApiException] = jsonFormat2(ApiException)

}
