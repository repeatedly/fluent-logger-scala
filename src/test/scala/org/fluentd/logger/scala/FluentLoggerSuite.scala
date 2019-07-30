//
// A Structured Logger for Fluent
//
// Copyright (C) 2012 OZAWA Tsuyoshi
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.fluentd.logger.scala

import org.fluentd.logger.scala.sender.{EventSerializer, MapSerializer}
import org.junit.runner.RunWith
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.scalatest.{BeforeAndAfterAll, Tag, FunSuite}
import org.scalatestplus.junit.JUnitRunner
import scala.collection.mutable.HashMap
import xerial.fluentd.FluentdStandalone

@RunWith(classOf[JUnitRunner])
class FluentLoggerSuite extends FunSuite with BeforeAndAfterAll {
  implicit val formats = Serialization.formats(NoTypeHints) + EventSerializer + MapSerializer

  var fluentd : Option[FluentdStandalone] = None
  var logger : FluentLogger = null

  override def beforeAll(): Unit = {
    // Start local fluentd server
    fluentd = Some(FluentdStandalone.start())
    logger = fluentd.map(fd => FluentLoggerFactory.getLogger("debug", "localhost", fd.port)).getOrElse {
      fail("Failed to start fluentd")
    }
  }

  override def afterAll(): Unit = {
    // Terminate the fluentd server, if started
    fluentd.map { _.stop }
  }


  test("test normal 1"){
    val data1 = new HashMap[String, String]()
    data1.put("k1", "v1")
    data1.put("k2", "v2")
    logger.log("test01", data1)
    
    val data2 = new HashMap[String, String]()
    val ts2 = System.currentTimeMillis
    data2.put("k3", "v3")
    data2.put("k4", "v4")
    logger.log("test01", data2)
    logger.log("test01", data2, ts2)
    
    val data3 = data2.toMap
    val ts3 = System.currentTimeMillis
    logger.log("test01", data3)
    logger.log("test01", data3, System.currentTimeMillis)
    logger.log("test01", "foo", "bar")
    logger.log("test01", "foo", "bar", ts3)
    
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }
  
  test("test big hash map") {
    val data = new HashMap[String, String]()
    for (i <- 1 to 100) {
      data.put("k"+i.toString, i.toString)
    }
    logger.log("test01", data)
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }
  
  test("test sending lots List objects") {
    val ev = new HashMap[String, List[String]]()
    var list: List[String] = List()
    for (i <- 1 to 100) {
      list = i.toString::list
      ev.put("key1", list)
      logger.log("test02", ev)
    }
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }
 
  test("test sending big List object") {
    val ev = new HashMap[String, List[String]]()
    for (i <- 1 to 100) {
      var list: List[String] = List()
   	  for (j <- 1 to 100) {
        list = j.toString::list
      }
      ev.put("key"+i.toString, list)
    }
    logger.log("test03", ev)
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }
  
  test("test sending nested Map object") {
    val ev = new HashMap[String, HashMap[String, String]]()
    val innerMap = HashMap[String, String]()
    for (i <- 1 to 100) {
      innerMap.put(i.toString, i.toString)
    }
    ev.put("inner", innerMap)
    logger.log("test04", ev)
  }
  
  test("test sending nested Map[String, Int] object") {
    val ev = new HashMap[String, HashMap[String, Int]]()
    val innerMap = HashMap[String, Int]()
    for (i <- 1 to 100) {
      innerMap.put(i.toString, i)
    }
    ev.put("inner", innerMap)
    logger.log("test05", ev)
  }
  
  test("test sending Set objects") {
    val ev = new HashMap[String, Set[Int]]()
    for (i <- 1 to 100) {
      var list: List[Int] = List()
   	  for (j <- 1 to 100) {
        list = j::list
      }
      ev.put("key"+i.toString, list.toSet)
    }
    logger.log("test01", ev)
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }
  
  test("test more nested objects") {
    val parent = new HashMap[String, HashMap[String, Set[Int]]]()
    val ev = new HashMap[String, Set[Int]]()
    for (i <- 1 to 100) {
      var list: List[Int] = List()
   	  for (j <- 1 to 100) {
        list = j::list
      }
      ev.put("key"+i.toString, list.toSet)
    }
    
    parent.put("key1", ev)
    logger.log("test01", parent)
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }

  test("test sending Loggable object") {
    val ts = System.currentTimeMillis
    val o = new Loggable {
      def toRecord = Map("key" -> "value")
    }
    logger.log("test01", o, ts)
    FluentLoggerFactory.flushAll()
    FluentLoggerFactory.closeAll()
  }

  test("close", Tag("close")) {
    def getLogger(tag:String) = {
      val port = fluentd.map(_.port).get
      FluentLoggerFactory.getLogger(tag, "localhost", port)
    }

    getLogger("tag1")
    getLogger("tag2")
    getLogger("tag3")

    FluentLoggerFactory.closeAll()
  }
}
