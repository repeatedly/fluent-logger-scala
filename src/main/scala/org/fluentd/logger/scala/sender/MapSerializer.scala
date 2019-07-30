package org.fluentd.logger.scala.sender

import org.json4s.{Extraction, Formats, MappingException, Serializer, TypeInfo}
import org.json4s.JsonAST.{JField, JObject, JValue}
import scala.collection.mutable.{Map => MutableMap}

object MapSerializer extends Serializer[Map[String, Any]] {
  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case m: Map[_, _] => 
      JObject(m.toMap.map({
      case (k, v) => JField(
        k match {
          case ks: String => ks
          case ks: Symbol => ks.name
          case ks: Any => ks.toString
        },
        Extraction.decompose(
          v match {
            case vs: MutableMap[_, _] => vs.toMap
            case _ => v
          }
        )
      )
    }).toList)
  }

  case class AnyMap(values: Map[String, Any]) 
  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Map[String, Any]] = {
    case x => throw new MappingException(s"Can't convert $x to Event")
  }
}
