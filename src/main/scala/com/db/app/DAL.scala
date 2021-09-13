package com.db.app

import scala.jdk.CollectionConverters._
import java.sql.{Connection, DriverManager}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try, Using}
import Utils._
import akka.http.scaladsl.model.StatusCodes
import com.db.app.Models.ApiException

trait DAL {
  def get(): Future[Map[String, String]]
  def uri: String
}

class SimpleDAL(volumePath: Option[String]) extends DAL {
  override def get(): Future[Map[String, String]] = {
    val dataPropertiesFuture = Future(Utils.loadProperties(volumePath))
    val responseFuture = dataPropertiesFuture.map(dataProperties =>
        dataProperties.entrySet().asScala.map(entry => (entry.getKey.toString, entry.getValue.toString)).toMap)
    responseFuture
  }

  override def uri: String = volumePath.getOrElse("/DefaultSimpleData.properties")
}

class VolumeDAL(volumePath: String) extends DAL {
  override def get(): Future[Map[String, String]] = Future.failed(new NotImplementedError())

  override def uri: String = ???
}

class MysqlDAL(mysqlHost: String, mysqlPort: String) extends DAL {
  val driver = "com.mysql.cj.jdbc.Driver"
  val url = s"jdbc:mysql://$mysqlHost:$mysqlPort/mysql"
  val username = "root"
  val password = "pass"
  Class.forName(driver)

  override def get(): Future[Map[String, String]] = {
    Future {
      Using(DriverManager.getConnection(url, username, password)) { connection =>
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT value, description FROM mysqltestdb.testdata")
        val data = resultSet.toLazyList.map(result => (result.getString("value"), result.getString("description"))).toMap
        data
      }
    }.flatMap {
      case Success(data) => Future.successful(data)
      case Failure(t) => Future.failed(ApiException(StatusCodes.InternalServerError.intValue, t.getMessage, s"${getClass.getName}"))
    }
  }

  override def uri: String = url
}

object MysqlDAL {
  def main(args: Array[String]): Unit = {

    // connect to the database named "mysql" on the localhost
    val driver = "com.mysql.cj.jdbc.Driver"
    //val url = "jdbc:mysql://0.0.0.0:3306/mysql"
    val url = "jdbc:mysql://192.168.49.2:30306/mysql"
    val username = "root"
    val password = "pass"

    // there's probably a better way to do this
    var connection:Connection = null

    try {
      // make the connection
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)

      // create the statement, and run the select query
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SELECT value, description FROM mysqltestdb.testdata")
      while ( resultSet.next() ) {
        val value = resultSet.getString("value")
        val description = resultSet.getString("description")
        println("value, description = " + value + ", " + description)
      }
    } catch {
      case e => e.printStackTrace
    }
    connection.close()
  }
}

class S3DAL extends DAL {
  override def get(): Future[Map[String, String]] = Future.failed(new NotImplementedError())

  override def uri: String = ???
}
