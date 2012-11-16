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


import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.fluentd.logger.util.MockFluentd;
import scala.collection.mutable.HashMap

@RunWith(classOf[JUnitRunner])
class FluentLoggerSuite extends FunSuite with BeforeAndAfter {
  
  // TODO: fix to use mock.
  test("test normal 1"){
    val logger = FluentLoggerFactory.getLogger("debug")
    val data1 = new HashMap[String, Object]();
    data1.put("k1", "v1");
    data1.put("k2", "v2");
    logger.log("test01", data1);
    val data2 = new HashMap[String, Object]();
    data2.put("k3", "v3");
    data2.put("k4", "v4");
    logger.log("test01", data2);
    logger.log("test01", data2, System.currentTimeMillis);
    val data3 = data2.toMap
    logger.log("test01", data3);
    logger.log("test01", data3, System.currentTimeMillis);
    logger.log("test01", "foo", "bar");
    logger.log("test01", "foo", "bar", System.currentTimeMillis);
    
    FluentLoggerFactory.flushAll
    FluentLoggerFactory.closeAll
  }
  
  test("test normal 2") {
    val logger0 = FluentLoggerFactory.getLogger("debug")
    for (i <- 1 to 100) {
      val data = new HashMap[String, Object]();
      data.put("k3", "v3");
      data.put("k4", "v4");
      logger0.log("test01", data);
    }
    FluentLoggerFactory.flushAll
    FluentLoggerFactory.closeAll
  }
  
  test("close") {
    FluentLoggerFactory.getLogger("tag1");
    FluentLoggerFactory.getLogger("tag2");
    FluentLoggerFactory.getLogger("tag3");
    FluentLoggerFactory.closeAll
  }

}
