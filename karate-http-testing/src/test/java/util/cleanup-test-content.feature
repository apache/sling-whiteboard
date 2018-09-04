# ------------------------------------------------------------------------
Feature: Cleanup content by DELETEing a set of paths
# The 'path' parameter must be passed in when calling, optionally
# using a table to delete multiple paths
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
Given path path
When method DELETE
Then status 204

Given path path
When method GET
Then status 404
