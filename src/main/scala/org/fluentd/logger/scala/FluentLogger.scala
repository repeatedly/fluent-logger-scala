package org.fluentd.logger.scala

import org.fluentd.logger.{FluentLogger => JavaFluentLogger}
import scala.collection.immutable.{Map => ImmutableMap}
import scala.collection.mutable.{Map => MutableMap}
import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap

class FluentLogger(javaLogger: JavaFluentLogger) {
  
  def log(label: String, key: String, value: Object): Boolean = {
    log(label, key, value, 0);
  }
  
  def log(label: String, key: String, value: Object, timestamp: Long): Boolean = {
    log(label, MutableMap(key -> value), timestamp);
  }
  
  def log(label: String, data: MutableMap[String, Object]): Boolean = {
    log(label, data.toMap, 0)
  }
  
  def log(label: String, data: ImmutableMap[String, Object]): Boolean = {
    log(label, data, 0)
  }
  
  def log(label: String, data: MutableMap[String, Object], timestamp: Long): Boolean = {
    log(label, data.toMap, timestamp)
  }
  
  def log(label: String, data: ImmutableMap[String, Object], timestamp: Long): Boolean = {
    val javaData: java.util.Map[String, Object] = data
    javaLogger.log(label, javaData, timestamp)
  }
  
  def flush() = javaLogger.flush()
  
  def close() = javaLogger.close()
  
  def getName: String = javaLogger.getName()
  
  override def toString: String = javaLogger.toString()
  
  override def finalize = javaLogger.finalize()
  
}
