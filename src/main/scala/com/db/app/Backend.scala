package com.db.app

import akka.http.scaladsl.model.HttpRequest
import com.db.app.LauncherBackend.applicationName
import com.db.app.Models.{ApiException, BackendResponse}
import com.typesafe.scalalogging.StrictLogging

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class Backend(address: String, port: String, version: String, dataPath: Option[String]) extends StrictLogging {

  private val backendApiKey = "key123"

  def getResponse(request: HttpRequest): Future[BackendResponse] = {
    logger.info(request.uri.toString())

    val dataPropertiesFuture = Future(Utils.loadProperties(dataPath))
    val responseFuture = dataPropertiesFuture.map(dataProperties =>
        BackendResponse(
          applicationName,
          com.db.app.language,
          version,
          address,
          port,
          dataProperties.entrySet().asScala.map(entry => (entry.getKey.toString, entry.getValue.toString)).toMap))
    responseFuture
  }

  def getResponseProtected(request: HttpRequest, apiKey: String): Future[BackendResponse] = {
    if (apiKey == backendApiKey) getResponse(request) else Future.failed(ApiException(401, "Unauthorized, key does not match", applicationName))
  }
}

