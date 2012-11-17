package models

import java.util.{Date}

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class $className$(id: Pk[Long] = NotAssigned, name: String)

object $className$ {
    
  /**
   * Parse a $className$ from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("$className;format="lower"$.id") ~
    get[String]("$className;format="lower"$.name") map {
      case id~name => Company(id, name)
    }
  }

  def findAll: Seq[$className$] = DB.withConnection { implicit connection =>
    SQL("select * from $className;format="lower"$ order by name").as($className$.simple *)
  }
  
}