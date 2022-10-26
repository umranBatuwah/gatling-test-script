package TimeService

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import javax.jms.Session

class CheckJsonData extends Simulation{

  val httpProtocol = http.baseUrl("http://localhost:5057/api")
    .acceptHeader("application/json")

  val scn = scenario("Checking name of json")
    .exec(http("getting specific time-detail")
    .get("/time-detail/56008")
    .check(jsonPath("$.joining_time.name").is("Search-data2")))

    .exec(http("getting all time-detail")
    .get("/time-detail")
    .check(jsonPath("$[1].id").saveAs("timeId")))
    .exec {Session => println(Session);Session}

    .exec(http("getting specific time-detail")
      .get("/time-detail/#{timeId}")
      .check(jsonPath("$.joining_time.name").is("Search-data2"))
    .check(bodyString.saveAs("responseBody")))
    .exec {Session => println(Session("responseBody").as[String]);Session}

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)


}
