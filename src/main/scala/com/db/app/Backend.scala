package com.db.app

import com.db.app.LauncherBackend.applicationName
import com.db.app.Models.{ApiException, BackendResponse}

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class Backend(address: String, port: String, version: String, dataPath: Option[String]) {

  private val backendApiKey = "key123"

  def getResponse(): Future[BackendResponse] = {
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

  def getResponseProtected(apiKey: String): Future[BackendResponse] = {
    if (apiKey == backendApiKey) getResponse() else Future.failed(ApiException(401, "Unauthorized, key does not match", applicationName))
  }
}

