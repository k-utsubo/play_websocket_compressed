package models

import akka.util._
import akka.actor._
import akka.pattern._
import com.redis._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent._

import scala.concurrent.duration._

case class Join(stockCodes:String)
case class Quit(stockCodes:String)
case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg:String)

object UserActor{
  implicit val timeout = Timeout(1 second)
  def join(codes:String):scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])]={
	val actor=Akka.system.actorOf(Props(new UserActor(codes)))
    (actor ? Join(codes)).map {
      case Connected(enumerator) =>
        println("UserActor.Connected")
        val iteratee = Iteratee.foreach[JsValue] {
          event => actor //TODO
        }.mapDone {
          _ => actor ! Quit(codes)
        }
        (iteratee, enumerator)

      case CannotConnect(error) =>
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))
        (iteratee, enumerator)
    }
  }
}

// 接続するユーザ別に作成されるActor、銘柄コードを複数持ち、Reids用のActorをSubscribeする
class UserActor(codes:String) extends Actor{

  def receive={
    // RedisからCallbackされてここに来る
    case RedisData(code:String,msg:String)=>notifyAll(code,msg)
  
    case Join(stockCodes) => {
      sender ! Connected(stockEnumerator)
      for(code<-codes.split(","))RedisActor.join(code,self)
    }

    case Quit(stockCodes) => {
      for(code<-codes.split(","))RedisActor.bye(code,self)
    }
  }
  var redisChannel: Option[Channel[JsValue]] = None
  
  // WebSocket用のなにか-->
  def onStart: Channel[JsValue] => Unit =  channel => redisChannel = Some(channel)
  def onError: (String, Input[JsValue]) => Unit =  (message, input) => println("UserActor.onError " + message)
  def onComplete =  self ! Quit("bye")
  
  val stockEnumerator = Concurrent.unicast[JsValue](onStart, onComplete, onError)
  //--<
  
  def notifyAll(code:String,data:String){
    // 実際にWebSocketでブラウザに送るデータ
    val msg=JsObject(
        Seq(
            "code"->JsString(code),
            "data"->JsString(Compress.encode(data)) // add encode
            )
        )
    redisChannel match{
      case Some(channel) => {
        channel.push(msg)
      }
      case _ => println("nothing")
    }     
  }
}