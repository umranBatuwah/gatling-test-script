package TimeService

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class Functions extends Simulation{

  val httpProtocol = http.baseUrl("http://localhost:5057/api")
    .acceptHeader("application/json")

  def getAllTimeDetail() = {
    repeat(3){
      exec(
        http("Get all time-detail")
          .get("/time-detail")
          .check(status.is(200))
      )
    }

  }

  def getbyId() ={

    repeat(3){
      exec(http("get time-detail by id")
        .get("/time-detail/56008")
        .check(status.in(200 to 210)))
    }
  }

  val scn =scenario("Getting timeDetails")
    .exec(getAllTimeDetail())
    .pause(4)
    .exec(getbyId())
    .pause(3)
    .repeat(3){
      getbyId()
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
