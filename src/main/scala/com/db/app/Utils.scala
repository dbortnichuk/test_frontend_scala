package com.db.app

import org.apache.commons.io.IOUtils.toByteArray

import java.io.{FileInputStream, InputStream}
import java.util.Properties
import scala.util.control.NonFatal

object Utils {

  def readResource(pathToResource: String): Array[Byte] = toByteArray(getClass.getResourceAsStream(pathToResource))

  def loadProperties(dataPath: Option[String]): Properties = {
    val dataProperties = new Properties()
    dataPath match {
      case None => {
        dataProperties.load(getClass.getResourceAsStream("/DefaultSimpleData.properties"))
      }
      case Some(path) => {
        var fis: InputStream = null
        try {
          fis = new FileInputStream(path)
          dataProperties.load(fis);
        } catch {
          case NonFatal(t) => throw t
        } finally {
          if ( fis != null) fis.close()
        }
      }
    }
    dataProperties
  }

}
