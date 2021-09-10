package com.db.app

import java.sql.{Connection, DriverManager}

trait DAL {
  def get(): String
}

class SimpleDAL(volumePath: Option[String]) extends DAL {
  override def get(): String = ???
}

class VolumeDAL(volumePath: String) extends DAL {
  override def get(): String = ???
}

class MysqlDAL extends DAL {
  override def get(): String = ???
}

object MysqlDAL {
  def main(args: Array[String]): Unit = {

    // connect to the database named "mysql" on the localhost
    val driver = "com.mysql.cj.jdbc.Driver"
    val url = "jdbc:mysql://0.0.0.0:3306/mysql"
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
  override def get(): String = ???
}
