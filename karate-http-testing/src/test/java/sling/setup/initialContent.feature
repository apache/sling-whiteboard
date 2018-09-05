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
# ------------------------------------------------------------------------
Examples:
  | contentPath | jsonPath | value |

  # Sling starter content
  | starter/css | jcr:primaryType | sling:Folder |
  | starter/css/bundle.css | jcr:createdBy | admin |
  | starter/index.html/jcr:content| jcr:mimeType| text/html |

  # OSGi console
  | system/console/bundles | data[0].symbolicName | org.apache.felix.framework |

  # Empty path means root
  | | sling:target | /starter/index.html |
  | | sling:resourceType | sling:redirect |