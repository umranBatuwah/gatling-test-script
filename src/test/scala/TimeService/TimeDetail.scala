package TimeService

import  io.gatling.core.Predef._
import  io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt


class TimeDetail extends Simulation{

  //http configuration
  val httpProtocol = http.baseUrl("http://localhost:5057/api")
    .acceptHeader("application/json")

  //Scenario Definition
  val scn = scenario("Time Service Testing 3 calls")
    .exec(http("Getting all time-details 1st time")
    .get("/time-detail")
    .check(status.is(200)))
    .pause(1)

    .exec(http("getting a specific time detail")
    .get("/time-detail/56002")
    .check(status.not(404),status.not(500)))
    .pause(300.milliseconds)

    .exec(http("getting all time-details 2nd time")
    .get("/time-detail")
    .check(status.in(200 to 210)))
    .pause(1,10)

  //load scenario
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
