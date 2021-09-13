package com.db.app

import com.typesafe.scalalogging.StrictLogging
import Logging._
import com.db.app.Models.ApiException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait Logging extends StrictLogging {

  def prependLabel(label: String, message: String): String = s"$label - $message"
  def prependPlaceholder(message: String): String = s"{} - $message"

  def logDebug(requestLabel: String, message: String): Unit = logger.debug(prependPlaceholder(message), requestLabel)
  def logDebug(message: String): Unit = logger.debug(prependLabel(defaultRequestLabel, message))

  def logInfo(requestLabel: String, message: String): Unit = logger.info(prependPlaceholder(message), requestLabel)
  def logInfo(message: String): Unit = logger.info(prependLabel(defaultRequestLabel, message))

  def logWarn(requestLabel: String, message: String): Unit = logger.warn(prependPlaceholder(message), requestLabel)
  def logWarn(message: String): Unit = logger.warn(prependLabel(defaultRequestLabel, message))

  def logError(requestLabel: String, message: String): Unit = logger.error(prependPlaceholder(message), requestLabel)
  def logError(message: String): Unit = logger.error(prependLabel(defaultRequestLabel, message))

  def withErrorLogging[T](task: Future[T])(implicit ec: ExecutionContext): Future[T] = {
    task.failed.foreach {
      case apiException: ApiException => logError(extractFullMessageTrace(apiException))
      case NonFatal(t) => {
        logError(t.getMessage)
        if(logger.underlying.isDebugEnabled) logDebug(extractFullMessageTrace(t))
      }
    }
    task
  }

}

object Logging {
  val headerValueDefaultClientRequestId = "no-request-id"
  val headerValueDefaultServiceId = "no-ols-tid"
  val headerValueDefaultCorrelationId = "no-cid"
  //val defaultRequestLabel: String = buildLabel(headerValueDefaultClientRequestId, headerValueDefaultServiceId, headerValueDefaultCorrelationId)
  val defaultRequestLabel: String = ""

  def convertToTraceString(t: Throwable, separator: String): String = t.getStackTrace.map(el => el.toString).mkString(separator)

  def extractFullMessageTrace(t: Throwable, separator: String = "\n\t"): String = {
    val exceptionStackMessage = convertToTraceString(t, separator)
    if(exceptionStackMessage.isEmpty) t.getMessage else s"${t.getMessage}$separator$exceptionStackMessage"
  }
}
