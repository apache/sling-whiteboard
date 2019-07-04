package org.apache.sling.graalvm;

import io.quarkus.test.junit.SubstrateTest;

@SubstrateTest
public class NativeSlingResourceIT extends SlingResourceTest {
    // Execute the same tests but in native mode.
}