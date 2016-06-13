package org.fluentd.logger.scala.sender

import org.fluentd.logger.sender.RawSocketSender
import java.nio.ByteBuffer
import java.net.InetSocketAddress
import org.fluentd.logger.sender.ExponentialDelayReconnector
import java.io.IOException
import java.net.Socket
import java.io.BufferedOutputStream
import scala.collection.Map
import org.json4s._
import org.json4s.native.Serialization

class ScalaRawSocketSender(h:String, p:Int, to:Int, bufCap:Int) 
    extends Sender {
  implicit val formats = DefaultFormats + EventSerializer + MapSerializer
  val LOG = java.util.logging.Logger.getLogger("ScalaRawSocketSender");
  val host = h
  val port = p
  val bufferCapacity= bufCap
  val timeout = to
  val name = "%s_%d_%d_%d".format(host, port, timeout, bufferCapacity)
  val pendings = ByteBuffer.allocate(bufferCapacity)
  val server = new InetSocketAddress(host, port)
  val reconnector = new ExponentialDelayReconnector()
  var socket:Socket = null;
  var out:BufferedOutputStream = null;
  open()
  
  def this(host:String, port:Int) {
    this(host, port, 3 * 1000, 8 * 1024 * 1024);
  } 
  
  def this() {
    this("localhost", 24224);
  }
  
  def open() = {
    try {
      connect();
    } catch {
      case e: IOException => 
        LOG.severe("Failed to connect fluentd: " + server.toString())
        LOG.severe("Connection will be retried"); 
        e.printStackTrace(); 
        close(); 
    }
  }
  
  def connect() = {
    try {
      socket = new Socket();  
      socket.connect(server);    
      socket.setSoTimeout(timeout); // the timeout value to be used in milliseconds 
      out = new BufferedOutputStream(socket.getOutputStream());   
      reconnector.clearErrorHistory(); 
    } catch {
      case e :IOException => 
        reconnector.addErrorHistory(System.currentTimeMillis());
        throw e; 
    }
  }
  
  def reconnect(forceReconnect: Boolean) {
    if (socket == null) {
      connect();
    } else if (forceReconnect || socket.isClosed() || (!socket.isConnected())) {
      close();
      connect();
    }
  }
  
  def close () = {
    // close output stream
    if (out != null) {
      try {
        out.close();
      } catch { 
        case e: IOException => // ignore
      } finally {
       out = null;
      }   
	}   

    // close socket
    if (socket != null) {
      try {
        socket.close();
      } catch {
        case e: IOException => // ignore
      } finally {
        socket = null;
      }   
	}   
  }

  def emit(tag: String, data: Map[String, Any]): Boolean = {
    emit(tag, System.currentTimeMillis() / 1000, data)
  }

  def emit(tag: String, timestamp: Long, data: Map[String, Any]): Boolean = {
    emit(new Event(tag, timestamp, data));
  }

  def emit(event: Event): Boolean = {
    //if (LOG.isLoggable(Level.FINE)) {
      //LOG.fine(String.format("Created %s", new Object[] { event }));
    //}

    try {
      // serialize tag, timestamp and data
      val json = Serialization.write(event)
      return send(json.getBytes("UTF-8"))
    } catch {
      case e: IOException =>
        LOG.severe("Cannot serialize event: " + event);
        e.printStackTrace();
        return false
    }
  }
  

  def send(bytes: Array[Byte]): Boolean = synchronized {
    // buffering
    if (pendings.position() + bytes.length > pendings.capacity()) {
      LOG.severe("Cannot send logs to " + server.toString());
      return false;
    }
    pendings.put(bytes);

    // suppress reconnection burst
    if (!reconnector.enableReconnection(System.currentTimeMillis())) {
      return true;
    }

    // send pending data
    flush();

    return true;
  }
  
  def getBuffer(): Array[Byte] = {
    val len = pendings.position();
    pendings.position(0);
    val ret = new Array[Byte](len);
    pendings.get(ret, 0, len);
    return ret;
  }

  def clearBuffer() = {
    pendings.clear();
  }

  def flush() = synchronized {
    try {
      // check whether connection is established or not
      reconnect(!reconnector.isErrorHistoryEmpty);
      // write data
      out.write(getBuffer());
      out.flush();
      clearBuffer();
    } catch  {
      case e: IOException =>
        LOG.throwing(this.getClass().getName(), "flush", e);
        reconnector.addErrorHistory(System.currentTimeMillis());
    }
  }

  def getName(): String = name
  
  override def toString(): String = {
    getName()
  }
}
