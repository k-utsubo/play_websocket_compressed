package models

import akka.actor._
import com.redis._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent._

case class RedisData(code:String,json:String)

case class SubMessage(user:ActorRef)
case class UnsubMessage(user:ActorRef)
case class ShutdownMessage()


object RedisActor{
  var actors=Map[String,ActorRef]()
  def join(code:String,user:ActorRef)= getActor(code) ! SubMessage(user)    
  def bye(code:String,user:ActorRef)= getActor(code) ! UnsubMessage(user)
  def die(code:String)={
    getActor(code) ! ShutdownMessage()
    actors=actors-code
  }
  
  // code別にActorを作成しMapで管理
  protected def getActor(code:String):ActorRef={
    actors.get(code) match{
      case Some(x)=> x // already exists
      case _ =>{ // create
        val x=ActorSystem("redis").actorOf(Props(new RedisActor(code)))
        actors+=code -> x
        x
      }
    }    
  }
}

// コード別にActorを作成し、ユーザ単位で複数登録しpublishでユーザに通知
class RedisActor(code:String) extends Actor {
  // Redis につなぐ際にもActor
  val system = ActorSystem(code)
  val r = new RedisClient("localhost",6379)

  val s = system.actorOf(Props(new Subscriber(r)))
  s ! Register(callback)

  def receive = {
    case SubMessage(user:ActorRef) =>{
      s ! Subscribe(Array(code))
      system.eventStream.subscribe(user,classOf[RedisData])
    }
    case UnsubMessage(user:ActorRef) =>{
      s ! Unsubscribe(Array(code))
      system.eventStream.unsubscribe(user)
    }
    case ShutdownMessage =>
      r.quit
      system.shutdown()
      system.awaitTermination()
  }

  // redisからcallback用
  def callback(pubsub: PubSubMessage) = pubsub match {
    case E(exception) => 
      println("Fatal error caused consumer dead. Please init new consumer reconnecting to master or connect to backup")
    case S(channel, no) => 
      println("subscribed to " + channel + " and count = " + no)
    case U(channel, no) => {
      r.unsubscribe(channel)
      println("unsubscribed from " + channel + " and count = " + no)
    }
    case M(channel, msg) =>
      msg match {
        case x =>
          // redisから値を受け取ったら、コード別に作成されたActorに対して一斉に配信する
          val code=channel       
          system.eventStream.publish(RedisData(code,x))
      }
  }
}
