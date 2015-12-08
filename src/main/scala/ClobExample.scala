import slick.driver._
import slick._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.JdbcProfile


object DatabaseSchema {

  trait Profile {
    val profile: slick.driver.JdbcProfile
  }

  trait Tables {
    this: Profile =>

    import profile.api._

    case class ItemMetadata(
      textValue: String,
      itemId: Option[Int]        = None,
      fieldId: Option[Int]       = None,
      lang: Option[String]       = None,
      place: Option[Int]         = None,
      authority: Option[String]  = None,
      confidence: Option[String] = None,
      id: Int = 0)

    case class Metadatavalues(tag: Tag) extends Table[ItemMetadata](tag, "METADATAVALUE") {

      def id         = column[Int]("METADATA_VALUE_ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
      def itemId     = column[Option[Int]]("ITEM_ID")
      def fieldId    = column[Option[Int]]("METADATA_FIELD_ID")
      def textValue  = column[String]("TEXT_VALUE", O.SqlType("CLOB"))
      def lang       = column[Option[String]]("TEXT_LANG")
      def place      = column[Option[Int]]("PLACE")
      def authority  = column[Option[String]]("AUTHORITY")
      def confidence = column[Option[String]]("CONFIDENCE")

      // Every table needs a * projection with the same type as the table's type parameter
      def * = (textValue, itemId, fieldId, lang, place, authority, confidence, id) <> (ItemMetadata.tupled, ItemMetadata.unapply)
    }

    val metadatavalues = TableQuery[Metadatavalues]
    lazy val ddl = metadatavalues.schema
  }
  case class Schema(val profile: JdbcProfile) extends Tables with Profile
}

import DatabaseSchema._

object ClobExample extends App {
  val schema = Schema(slick.driver.H2Driver)
  import schema._, profile.api._

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  val db = Database.forConfig("clob-example")

  val testData = List(
      ItemMetadata("bog"),
      ItemMetadata("cog"),
      ItemMetadata("dog"),
      ItemMetadata("frog"),
      ItemMetadata("mog"),
      ItemMetadata("oog")
  )

  val action = for {
    _      <- ddl.create
    _      <- metadatavalues ++= testData
    values <- metadatavalues.filter(_.textValue === "dog").result
  } yield values

  exec(action).foreach { m =>
    println(s"""id: ${m.id}
               |itemId:      ${m.itemId}
               | fieldid:    ${m.fieldId}
               | textvalue:  ${m.textValue}
               | lang:       ${m.lang}
               | place:      ${m.place}
               | authority:  ${m.authority}
               | confidence: ${m.confidence}""".stripMargin)

  }

}