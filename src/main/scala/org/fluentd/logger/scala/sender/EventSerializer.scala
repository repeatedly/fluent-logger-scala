package org.fluentd.logger.scala.sender

import org.json4s.{Extraction, Formats, MappingException, Serializer, TypeInfo}
import org.json4s.JsonAST.{JArray, JInt, JNothing, JObject, JString, JValue}

object EventSerializer extends Serializer[Event] {
  private val EventClass = classOf[Event]
  
  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case ev: Event =>
      JArray(List(JString(ev.tag), JInt(ev.time), Extraction.decompose(ev.record)))
  }

  /* This method is only used for testing. */
  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Event] = {
    case (TypeInfo(EventClass, _), json) => json match {
      case JArray(JString(tag) :: JInt(time) :: JObject(obj) :: Nil) =>
        Event(tag, time.toLong, null)
      case JNothing =>
        Event(null, 0, null)
      case x => throw new MappingException(s"Can't convert $x to Event")
    }
  }

}
