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

  case class FrontendModel(
                            override val application: String,
                            override val lang: String,
                            override val version: String,
                            override val address: String,
                            override val port: String,
                            upstream: Option[BackendModel] = None
                          ) extends ResponseModel


  case class BackendModel(override val application: String,
                          override val lang: String,
                          override val version: String,
                          override val address: String,
                          override val port: String,
                          data: Option[String] = None) extends ResponseModel




}
