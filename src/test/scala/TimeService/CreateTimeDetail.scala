package TimeService

import io.gatling.core.Predef._
import io.gatling.core.session.Session
import io.gatling.http.Predef._
import io.gatling.javaapi.core.CheckBuilder.JsonPath


class CreateTimeDetail extends Simulation {

  val httpProtocol = http.baseUrl("http://127.0.1.1:5057/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  def createTimeDetail() = {
    exec(
      http("Create time-detail")
        .post("/service-detail")
        .body(StringBody("{    \"joining_time\": {\n    \"name\": \"Search-data2\",\n    \"description\": \"Common Search Service ES Database\",\n    \"associatedOU\": \"Synectiks\",\n    \"associatedDept\": \"HR\",\n    \"associatedProduct\": \"HRMS\",\n    \"associatedEnv\": \"PROD\",\n    \"associatedLandingZone\": \"567373484\",\n    \"associatedProductEnclave\": \"567373484-VPC1\",\n    \"associatedCluster\": \"567373484-VPC1-Cluster1\",\n    \"serviceNature\": \"Common\",\n    \"associatedCommonService\": \"Search\",\n    \"associatedBusinessService\": \"\",\n    \"serviceType\": \"Data\",\n    \"serviceHostingType\": \"Cluster\",\n    \"associatedClusterNamespace\": \"HRMS\",\n    \"associatedManagedCloudServiceLocation\": \"\",\n    \"associatedCloudElementId\": \"567373484-VPC1-Cluster1\",\n    \"associatedGlobalServiceLocation\": \"\",\n    \"stats\": {\n        \"totalCostSoFar\": \"107\",\n        \"lastDayCost\": \"97\",\n        \"lastWeekCost\": \"0\",\n        \"lastMonthCost\": \"10\"\n    },\n    \"performance\": {\n        \"score\": 17\n    },\n    \"availability\": {\n        \"score\": 83\n    },\n    \"security\": {\n        \"score\": 9\n    },\n    \"dataProtection\": {\n        \"score\": 24\n    },\n    \"userExperiance\": {\n        \"score\": 44\n    }\n}}"))
        .check(bodyString.saveAs("responseBody"))
    )
      .exec {Session => println(Session("responseBody").as[String]);Session}
  }

  val scn = scenario("Creating Time-Detail")
    .exec(createTimeDetail())

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}