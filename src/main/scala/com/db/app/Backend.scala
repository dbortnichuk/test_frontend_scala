package com.db.app

import akka.http.scaladsl.model.HttpRequest
import com.db.app.LauncherBackend.applicationName
import com.db.app.Models.{ApiException, BackendResponse}
import com.typesafe.scalalogging.StrictLogging

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class Backend(address: String,
              port: String,
              version: String,
              simpleDataPath: Option[String],
              volumeDataPath: String,
             ) extends StrictLogging {

  private val backendApiKey = "key123"

  private val dataSourceRegistry = Map(
    ParamDataSourceSimpleVal -> new SimpleDAL(simpleDataPath),
    ParamDataSourceVolumeVal -> new VolumeDAL(volumeDataPath),
    ParamDataSourceMysqlVal -> new MysqlDAL(),
    ParamDataSourceS3Val -> new S3DAL()
  )

  def getResponse(request: HttpRequest, dataSourceOption: Option[String]): Future[BackendResponse] = {
    logger.info(request.uri.toString())

    val dataSource = dataSourceRegistry(dataSourceOption.getOrElse(ParamDataSourceSimpleVal))

    val dataPropertiesFuture = Future(Utils.loadProperties(simpleDataPath))
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

  def getResponseProtected(request: HttpRequest, dataSourceOption: Option[String], apiKey: String): Future[BackendResponse] = {
    if (apiKey == backendApiKey) getResponse(request, dataSourceOption)
    else Future.failed(ApiException(401, "Unauthorized, key does not match", applicationName))
  }
}

