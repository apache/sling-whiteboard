# Apache Sling JSON Servlet Support

Sling excels at delivering content and experiences, however creating APIs isn't as smooth of an experience.

Apache Sling JSON Servlet support enables developers to easily create REST API's on top of Apache Sling. This library includes:

 - Implementation of non-Resource-based Servlets for creating OSGi Whiteboard APIs
 - Built-in methods for deserializing requests and serializing responses to JSON
 - Built-in support for [RFC-7807 JSON Problem](https://datatracker.ietf.org/doc/html/rfc7807) responses
 - Extensions of Java's default HttpServlet which ensures exceptions are returned as JSON Problem responses with reasonable HTTP codes

## Use

After installing the bundle, you can create Servlets using the OSGi HTTP whiteboard context:

    @Component(service = { Servlet.class })
    @HttpWhiteboardContextSelect("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=com.company.restapi)")
    @HttpWhiteboardServletPattern("/myapi/*")
    public class MyApiServlet extends JacksonJsonServlet {

        protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response)
                throws ServletException, IOException {
            Map<String, Object> properties = super.readRequestBody(request, new TypeReference<Map<String, Object>>() {
            });
            String name = properties.get("name");
            if (StringUtils.isBlank(name)) {
                super.sendProblemResponse(response, ProblemBuilder.get().withStatus(400).withDetails("Please provide a name").build());
            } else {
                Map responseBody = Map.of("message", "Greetings " + name);
                super.sendJsonResponse(response, responseBody);
            }
        }
    }

