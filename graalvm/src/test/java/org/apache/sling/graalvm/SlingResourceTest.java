package org.apache.sling.graalvm;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class SlingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/sling")
          .then()
             .statusCode(200)
             .body(containsString("Hello, at"));
    }

}