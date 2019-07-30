package org.fluentd.logger.scala.sender

import java.io.{BufferedOutputStream, IOException}
import java.net.{InetSocketAddress, Socket}
import java.nio.{Buffer, ByteBuffer}
import org.fluentd.logger.sender.ExponentialDelayReconnector
import org.json4s._
import org.json4s.native.Serialization
import scala.collection.Map

class ScalaRawSocketSender(h: String, p: Int, to: Int, bufCap: Int)
    extends Sender {
  implicit val formats: Formats = DefaultFormats + EventSerializer + MapSerializer
  val LOG = java.util.logging.Logger.getLogger("ScalaRawSocketSender")
  val host = h
  val port = p
  val bufferCapacity = bufCap
  val timeout = to
  val name = "%s_%d_%d_%d".format(host, port, timeout, bufferCapacity)
  val pendings = ByteBuffer.allocate(bufferCapacity)
  val server = new InetSocketAddress(host, port)
  val reconnector = new ExponentialDelayReconnector()
  var socket: Socket = null
  var out: BufferedOutputStream = null
  open()

  def this(host:String, port:Int) {
    this(host, port, 3 * 1000, 8 * 1024 * 1024)
  } 

  def this() {
    this("localhost", 24224)
  }

  def open(): Unit = {
    try {
      connect()
    } catch {
      case e: IOException => 
        LOG.severe(s"Failed to connect fluentd: $server")
        LOG.severe("Connection will be retried")
        e.printStackTrace()
        close()
    }
  }

  def connect(): Unit = {
    try {
      socket = new Socket()
      socket.connect(server)
      socket.setSoTimeout(timeout) // the timeout value to be used in milliseconds
      out = new BufferedOutputStream(socket.getOutputStream())
      reconnector.clearErrorHistory()
    } catch {
      case e :IOException =>
        reconnector.addErrorHistory(System.currentTimeMillis())
        throw e
    }
  }

  def reconnect(forceReconnect: Boolean): Unit = {
    if (socket == null) {
      connect()
    } else if (forceReconnect || socket.isClosed() || (!socket.isConnected())) {
      close()
      connect()
    }
  }

  def close(): Unit = {
    // close output stream
    if (out != null) {
      try {
        out.close()
      } catch { 
        case e: IOException => // ignore
      } finally {
       out = null
      }
    }

    // close socket
    if (socket != null) {
      try {
        socket.close()
      } catch {
        case e: IOException => // ignore
      } finally {
        socket = null
      }
    }
  }

  def emit(tag: String, data: Map[String, Any]): Boolean = {
    emit(tag, System.currentTimeMillis() / 1000, data)
  }

  def emit(tag: String, timestamp: Long, data: Map[String, Any]): Boolean = {
    emit(Event(tag, timestamp, data))
  }

  def emit(event: Event): Boolean = {
    //if (LOG.isLoggable(Level.FINE)) {
      //LOG.fine(String.format("Created %s", new Object[] { event }))
    //}

    try {
      // serialize tag, timestamp and data
      val json = Serialization.write(event)
      send(json.getBytes("UTF-8"))
    } catch {
      case e: IOException =>
        LOG.severe(s"Cannot serialize event: $event")
        e.printStackTrace()
        false
    }
  }

  def send(bytes: Array[Byte]): Boolean = synchronized {
    // buffering
    if (pendings.position() + bytes.length > pendings.capacity()) {
      LOG.severe(s"Cannot send logs to $server")
      false
    } else {
      pendings.put(bytes)

      if (reconnector.enableReconnection(System.currentTimeMillis())) {
        // send pending data
        flush()
      }

      true
    }
  }

  def getBuffer(): Array[Byte] = {
    val len = pendings.position()
    pendings.position(0)
    val ret = new Array[Byte](len)
    pendings.get(ret, 0, len)
    ret
  }

  def clearBuffer(): Buffer = {
    pendings.clear()
  }

  def flush(): Unit = synchronized {
    try {
      // check whether connection is established or not
      reconnect(!reconnector.isErrorHistoryEmpty)
      // write data
      out.write(getBuffer())
      out.flush()
      clearBuffer()
    } catch {
      case e: IOException =>
        LOG.throwing(this.getClass.getName, "flush", e)
        reconnector.addErrorHistory(System.currentTimeMillis())
    }
  }

  def getName(): String = name

  override def toString: String = {
    getName()
  }
}
