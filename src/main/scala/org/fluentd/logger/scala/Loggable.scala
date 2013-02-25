package org.fluentd.logger.scala

trait Loggable {
  def toRecord: Map[String, Any]
}
