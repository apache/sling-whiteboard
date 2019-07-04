package org.apache.sling.graalvm.http;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import javax.ws.rs.core.MediaType;

@QuarkusTest
public class SlingResourceTest {

    @Test
    public void testSlingResourceEndpoint() {
        final String prefix = "/sling/";
        final String path = "chouc/route";
        final String resourceType = "mock/resource";

        given()
          .when().get(prefix + path)
          .then()
             .statusCode(200)
             .contentType(MediaType.APPLICATION_JSON)
             .body("path", equalTo(prefix + path))
             .body("resourceType", equalTo(resourceType));
    }

}