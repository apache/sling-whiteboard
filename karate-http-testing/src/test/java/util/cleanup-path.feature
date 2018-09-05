# ------------------------------------------------------------------------
Feature: DELETE a path and verify that it's gone
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

# ------------------------------------------------------------------------
Scenario: Delete a given path and verify that it's gone
# ------------------------------------------------------------------------
Given path pathToDelete
When method DELETE
Then status 204

# Use HEAD to avoid an XML parsing warning
Given path pathToDelete
When method HEAD
Then status 404