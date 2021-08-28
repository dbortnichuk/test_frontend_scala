package com.db.app

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import com.db.app.Models.{BackendModel, FrontendModel}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val BackendModelFormat: RootJsonFormat[BackendModel] = jsonFormat6(BackendModel)
  implicit val FrontendModelFormat: RootJsonFormat[FrontendModel] = jsonFormat6(FrontendModel)

}
