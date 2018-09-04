# ------------------------------------------------------------------------
Feature: Upload an image in Sling and check the result
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:sling/util/basic-auth-header.js') { username: 'admin', password: 'admin' }

* def testID = '' + java.util.UUID.randomUUID()
* def testFolderPath = '/uploadImageTest/folder_' + testID
* def filename = 'file_' + testID

# ------------------------------------------------------------------------
Scenario: Check access to the Sling instance under test
# ------------------------------------------------------------------------
Given path '/.json'
When method GET
Then status 200

# ------------------------------------------------------------------------
Scenario: Upload an image, read back and check
# ------------------------------------------------------------------------

# Create a resource
Given path testFolderPath + '/*'
And multipart field file = read("classpath:images/testimage.jpg")
And multipart field name = filename
When method POST
Then status 201

# The Location header provides the path where the resource was created
* def imagePath = responseHeaders['Location'][0]

# Read metadata back
Given path imagePath + '.tidy.5.json'
When method GET
Then status 200
And match response.jcr:primaryType == 'nt:unstructured'
And match response.name == filename
And match response.file.jcr:primaryType == 'nt:resource'
And match response.file.jcr:mimeType == 'application/octet-stream'

# Delete and verify that the resource is gone
Given path imagePath
When method DELETE
Then status 204

Given path imagePath
When method GET
Then status 404

# Cleanup test folder
Given path testFolderPath
When method DELETE
Then status 204

Given path testFolderPath
When method GET
Then status 404