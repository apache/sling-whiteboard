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

  // Declare path patterns used by our Karate tests, to group similar
  // requests in the Gatling report. Optionally add delays to specific
  // HTTP request methods.
  val protocol = karateProtocol(
    "/createContentTest/{folder}/{testResource}" -> pauseFor("post" -> 0),
    "/createContentTest/{folder}" -> pauseFor("post" -> 0),
    "/importContentTest/{folder}/{testResource}" -> pauseFor("post" -> 0),
    "/importContentTest/{folder}" -> pauseFor("post" -> 0),
    "/importContentTest/{folder}/{testResource}/{file}" -> pauseFor("get" -> 0),
    "/uploadImageTest/{folder}" -> pauseFor("delete" -> 0),
    "/uploadImageTest/{folder}/*" -> pauseFor("get" -> 0),
    "/uploadImageTest/{folder}/{file}" -> pauseFor("get" -> 0),
    "/uploadImageTest/{folder}/{file}/{details}" -> pauseFor("get" -> 0),
    "/uploadImageTest/{folder}/{file}/file/jcr:data" -> pauseFor("get" -> 0)
  )

  // Which Karate features do we want to test?
  val createContent = scenario("create")
    .exec(
      karateFeature("classpath:sling/postservlet/createContent.feature")
    )
  val importContent = scenario("import")
    .exec(
      karateFeature("classpath:sling/postservlet/importContent.feature")
    )
  val uploadImage = scenario("upload")
    .exec(
      karateFeature("classpath:sling/filestorage/uploadImage.feature")
    )

    // Define Gatling load models
  setUp(
    createContent.inject(rampUsers(75) over (5 seconds)).protocols(protocol),
    importContent.inject(rampUsers(125) over (3 seconds)).protocols(protocol),
    uploadImage.inject(rampUsers(50) over (1 seconds)).protocols(protocol)
  )
}