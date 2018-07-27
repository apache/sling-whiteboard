Feature: create content using the Sling POST Servlet

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

# TODO for now you need to start Sling manually
# TODO get this from the environment

* url 'http://localhost:8080'

# Use admin credentials for all requests
* def encodedAuth = call read('basic-auth.js') { username: 'admin', password: 'admin' }
* configure headers = { 'Authorization' : #(encodedAuth) }

# ------------------------------------------------------------------------
Scenario: Check access to the Sling instance under test
# ------------------------------------------------------------------------
Given path '/.json'
When method get
Then status 200

# ------------------------------------------------------------------------
Scenario: Create a resource with a POST and read back with a GET
# ------------------------------------------------------------------------
* def testID = '' + java.util.UUID.randomUUID()
* def testTitle = 'Title for the First Resource at ' + testID

Given url 'http://localhost:8080/tmp/*'
And form field title = testTitle
And form field anotherField = testID
When method POST
Then status 201

# The Location header indicates where the resource was created
# TODO use a variable for the base URL
* def newResourceURL = 'http://localhost:8080' + responseHeaders['Location'][0]

Given url newResourceURL + '.json'
When method get
Then status 200
And match response.title == testTitle
And match response.anotherField == testID