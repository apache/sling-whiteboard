# Apache Sling JaxRs Json Support

Sling excels at delivering content and experiences, however creating APIs isn't as smooth of an experience.

This is a POC based on the [aries-jax-rs-whiteboard](../aries-jax-rs-whiteboard/) showing how Aries Jax Rs Whiteboard can be incorporated into any modern Sling application and be used to create RESTful applications.

This implementation includes features to support serializing and deserialzing responses from JSON and JSON Problem support including the ability to map ThrowableProblems into responses.

## Use

Run:

    mvn clean install

This will build the code and run the IT which installs the feature models and additional bundles into the local Apache Sling instance and then verifies that the service is responding as expected.

If you want to change the default client configuration, provide following the properties:

 - sling.it.host
 - sling.it.username
 - sling.it.password

E.g.:

    mvn clean install -Dsling.it.host=http://localhost:4502