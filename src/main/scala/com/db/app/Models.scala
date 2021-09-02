package com.db.app

import com.db.app.Models.ResponseModel

object Models {

  sealed trait ResponseModel {
    def application: String
    def lang: String
    def version: String
    def address: String
    def port: String
  }

  case class FrontendResponse(
                            override val application: String,
                            override val lang: String,
                            override val version: String,
                            override val address: String,
                            override val port: String,
                            upstream: Option[BackendResponse] = None
                          ) extends ResponseModel


  case class BackendResponse(override val application: String,
                             override val lang: String,
                             override val version: String,
                             override val address: String,
                             override val port: String,
                             data: Map[String, String] = Map.empty) extends ResponseModel


  case class ApiException(status: Int, msg: String) extends RuntimeException(msg)

}
