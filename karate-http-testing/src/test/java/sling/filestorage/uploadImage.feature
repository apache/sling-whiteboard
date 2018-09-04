# ------------------------------------------------------------------------
Feature: Upload an image in Sling and check the result
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

* def testID = '' + java.util.UUID.randomUUID()
* def testFolderPath = '/uploadImageTest/folder_' + testID
* def filename = 'file_' + testID

# Sling instance ready?
* def unused = call read('classpath:util/sling-ready.feature')

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

# Cleanup test content
* table paths
  | path |
  | imagePath |
  | testFolderPath |
* def result = call read('classpath:util/cleanup-test-content.feature') paths