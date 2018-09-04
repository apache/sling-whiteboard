# ------------------------------------------------------------------------
Feature: Check access to the Sling instance under test
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

# ------------------------------------------------------------------------
Scenario: Check access to HTTP root
# ------------------------------------------------------------------------
Given path '/.json'
When method GET
Then status 200
