package models

import java.util.zip._
import java.io._
import org.apache.commons.codec.binary._

object Compress {
  def encode(str:String):String={
    val out = new ByteArrayOutputStream()
    val defl = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION, true))
    defl.write(str.getBytes())
    defl.close()
    val compressed = Base64.encodeBase64(out.toByteArray())
    new String(compressed)    
  }
}