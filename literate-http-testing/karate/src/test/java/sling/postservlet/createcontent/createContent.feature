# ------------------------------------------------------------------------
Feature: create content using the Sling POST Servlet
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('basic-auth-header.js') { username: 'admin', password: 'admin' }

* def testID = '' + java.util.UUID.randomUUID()
* def testFolderPath = '/createContentTest' + testID

# ------------------------------------------------------------------------
Scenario: Check access to the Sling instance under test
# ------------------------------------------------------------------------
Given path '/.json'
When method GET
Then status 200

# ------------------------------------------------------------------------
Scenario: Create a resource, update, read back, delete
# ------------------------------------------------------------------------

# Create a resource
Given path testFolderPath + '/*'
And form field f1 = 'v1A' + testID
And form field f2 = 'v2A'
When method POST
Then status 201

# The Location header provides the path where the resource was created
* def resourcePath = responseHeaders['Location'][0]

# Read back
Given path resourcePath + '.json'
When method GET
Then status 200
And match response.f1 == 'v1A' + testID
And match response.f2 == 'v2A'

# Overwrite one field and add a new one
Given path resourcePath
And form field f2 = 'v2B'
And form field f3 = 'v3B'
When method POST
Then status 200

# Read modified resource back
Given path resourcePath + '.json'
When method GET
Then status 200
And match response.f1 == 'v1A' + testID
And match response.f2 == 'v2B'
And match response.f3 == 'v3B'

# Delete and verify that the resource is gone
Given path resourcePath
When method DELETE
Then status 204

Given path resourcePath
When method GET
Then status 404

# Cleanup test folder
Given path testFolderPath
When method DELETE
Then status 204

Given path testFolderPath
When method GET
Then status 404