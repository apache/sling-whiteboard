# ------------------------------------------------------------------------
@importcontent @postservlet
Feature: Import content using the Sling POST Servlet
# ------------------------------------------------------------------------

Background:
* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

# Sling instance ready?
* def unused = call read('classpath:util/sling-ready.feature')

* def testID = '' + java.util.UUID.randomUUID()
* def testFolderPath = 'importContentTest/' + testID

# ------------------------------------------------------------------------
Scenario: Create the parent folder, import JSON content, verify and delete
# ------------------------------------------------------------------------

* def newContent = 
"""
{ 
    'jcr:primaryType' : 'nt:unstructured', 
    p1 : '#(testID)', 
    p2: [ 'a', 'b', '#(testID)' ] 
}
"""

# Create parent folder
Given path testFolderPath, testID
And request ""
When method POST
Then status 201
And def parentFolder = responseHeaders['Location'][0]

# Import content
Given path parentFolder
And form field :operation = 'import'
And form field :contentType = 'json'
And form field :name = testID
And form field :content = newContent
When method POST
Then status 201

# Verify imported content
Given path parentFolder, testID + ".json"
When method GET
Then status 200
And match $ == newContent
And match $.p2[2] == testID

# Cleanup test content
* def toDelete = ([ testFolderPath + '/' + testID, testFolderPath ])
* def result = call read('classpath:util/cleanup-paths.js') toDelete