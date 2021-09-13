package com.db.app

import akka.http.scaladsl.model.HttpRequest
import com.db.app.LauncherBackend.applicationName
import com.db.app.Models.{ApiException, BackendResponse}
import scala.concurrent.Future

class Backend(address: String,
              port: String,
              version: String,
              simpleDataPath: Option[String],
              volumeDataPath: String,
              mysqlHost: String,
              mysqlPort: String
             ) extends Logging {

  private val backendApiKey = "key123"

  private val dataSourceRegistry = Map(
    ParamDataSourceSimpleVal -> new SimpleDAL(simpleDataPath),
    ParamDataSourceVolumeVal -> new VolumeDAL(volumeDataPath),
    ParamDataSourceMysqlVal -> new MysqlDAL(mysqlHost, mysqlPort),
    ParamDataSourceS3Val -> new S3DAL()
  )

  def getResponse(request: HttpRequest, dataSourceOption: Option[String]): Future[BackendResponse] = {
    val dataSource = dataSourceRegistry.getOrElse(
      dataSourceOption.getOrElse(ParamDataSourceSimpleVal), // if param not provided
      dataSourceRegistry(ParamDataSourceSimpleVal) // if param val not known
    )
    logger.info(s"${request.uri.toString()} -> ${dataSource.uri}")
    val responseFuture = withErrorLogging(dataSource.get().map(data =>
      BackendResponse(applicationName, com.db.app.language, version, address, port, data)))
    responseFuture
  }

  def getResponseProtected(request: HttpRequest, dataSourceOption: Option[String], apiKey: String): Future[BackendResponse] = {
    if (apiKey == backendApiKey) getResponse(request, dataSourceOption)
    else Future.failed(ApiException(401, "Unauthorized, key does not match", applicationName))
  }
}

