package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.iteratee._

import models._

import akka.actor._
import scala.concurrent.duration._

object Application extends Controller {
  
  def board(code:Option[String]) = Action{ implicit request =>
    code match{
      case Some(x)=>Ok(views.html.board(x))
    }
  }
    
  def boardJs(code:String) = Action{implicit request =>
    Ok(views.js.board(code))
  }
  def data(code:String)=WebSocket.async[JsValue] { request =>
    UserActor.join(code)
  }
}