package sling

import com.intuit.karate.gatling.PreDef.{pauseFor, _}
import io.gatling.core.Predef._

import scala.concurrent.duration._

/* Example Gatling performance tests which reuse Karate scenarios.
 *
 * Run with 
 *  mvn clean test-compile gatling:test
 *
 * And see the output for the Gatling report filename.
 */
class PostServletSimulation extends Simulation {

  val protocol = karateProtocol(
    "/createContentTest/*" -> pauseFor("post" -> 2, "get" -> 5, "delete" -> 2),
    "/importContentTest/*" -> pauseFor("post" -> 7, "get" -> 15, "delete" -> 50),
  )

  val createContent = scenario("create")
    .exec(
      karateFeature("classpath:sling/postservlet/createContent.feature")
    )
  val importContent = scenario("import")
    .exec(
      karateFeature("classpath:sling/postservlet/importContent.feature")
    )

  setUp(
    createContent.inject(rampUsers(75) over (5 seconds)).protocols(protocol),
    importContent.inject(rampUsers(125) over (3 seconds)).protocols(protocol)
  )
}