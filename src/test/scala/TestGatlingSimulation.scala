import com.datastax.driver.core._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._
import org.fluttercode.datafactory.impl.DataFactory

import scala.concurrent.duration.DurationInt
import scala.util.Random


class TestGatlingSimulation extends Simulation {


  val cluster = Cluster.builder().addContactPoints("127.0.0.1")
    //.withCredentials("cassandra", "xxx")
    .build()

  val session = cluster.connect()
  val cqlConfig = cql.session(session)

  val random = new Random()

  val df = new DataFactory

  def getRandomStr(lenght: Int) = Random.alphanumeric take lenght mkString ("")

  def getRandom[A](list: Array[A]): A = list(random.nextInt(list.length))

  val feeder = Iterator.continually(
    // this feader will "feed" random data into our Sessions
    Map(
      "randomString" -> getRandomStr(4),
      "randomWord" -> df.getRandomWord(4, 8).replace("'", "''").replace("\"", "\\\\\\\"").replace(" ", "\\\\ ")
    ))


  val scn = scenario("search").repeat(1) { //Name your scenario
    feed(feeder)
     .exec(cql("simple search")
        .execute("""select * from atwater.product where solr_query = '{"q":"name:${randomString}"}'"""))
      .exec(cql("match search")
        .execute("""select * from atwater.product where solr_query = '{"q":"name:${randomWord}"}'"""))
      .exec(cql("match search2")
        .execute("""select * from atwater.product where solr_query = '{"q":"brandname:${randomWord}"}'"""))
      .exec(cql("match search3")
        .execute("""select * from atwater.product where solr_query = '{"q":"short_description:${randomWord}"}'"""))
     // .exec(cql("facet search")
     //  .execute("""select * from atwater.product where solr_query = '{"q":"*:*","fq":"name:${randomWord}", "facet":{"field":["product_tags","brandname"],"limit":10}}'"""))


  }

  setUp(scn.inject(rampUsersPerSec(200) to 1100 during (60 seconds)))
    .protocols(cqlConfig)

  after(cluster.close()) // close session and stop associated threads started by the Java/Scala driver

}