# ------------------------------------------------------------------------
@slingsetup @initialcontent
Feature: Test the Sling initial content and demonstrate Scenario Outline
# ------------------------------------------------------------------------

Background:
* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

# Sling instance ready?
* eval karate.call('classpath:util/sling-ready.feature')

# ------------------------------------------------------------------------
Scenario Outline: Validate Initial Content
# ------------------------------------------------------------------------

Given path '<contentPath>' + '.json'
When method GET
Then status 200
And match $.<jsonPath> == "<value>"

# ------------------------------------------------------------------------
# Data values used by the Scenario outline
# These values work for the Sling 10 release
# ------------------------------------------------------------------------
Examples:
  | contentPath | jsonPath | value |

  # Sling starter content
  | home/users | jcr:primaryType | rep:AuthorizableFolder |
  | sling.css | jcr:createdBy | admin |
  | htl/repl| sling:resourceType | repl/components/repl |

  # OSGi console
  | system/console/bundles | data[0].symbolicName | org.apache.felix.framework |

  # Empty path means root
  | | sling:target | /index.html |
  | | sling:resourceType | sling:redirect |