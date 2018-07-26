Feature: create content using the Sling POST Servlet

Background:

# TODO for now you need to start Sling manually
# TODO get this from the environment

* url 'http://localhost:8080'

# Use admin:admin credentials for all requests
* configure headers = { 'Authorization' : 'Basic YWRtaW46YWRtaW4=' }

Scenario: get the root resource

Given path '/.json'
When method get
Then status 200

Scenario: create a content resource and verify its output

* def id = java.util.UUID.randomUUID()
* def title = 'Title for the First Resource at ' + id

Given url 'http://localhost:8080/tmp/' + id
And form field title = title
And form field const = 'const42'
When method POST
Then status 201

# TODO use a variable for the base URL

* def location = 'http://localhost:8080' + responseHeaders['Location'][0]

Given url location + '.json'
When method get
Then status 200
Then match response.title == title
Then match response.const == 'const42'