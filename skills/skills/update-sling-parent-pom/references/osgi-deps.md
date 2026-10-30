# Individual management of OSGi dependencies

Remove dependencies of aggregate OSGi artifacts:
- org.osgi:osgi.core
- org.osgi:osgi.cmpn

In case of compilation failures use the following individual dependencies (already managed in the parent pom):

- org.osgi:org.osgi.framework
- org.osgi:org.osgi.util.tracker
- org.osgi:org.osgi.dto
- org.osgi:org.osgi.service.url
- org.osgi:org.osgi.resource
- org.osgi:org.osgi.service.cm
- org.osgi:org.osgi.service.component
- org.osgi:org.osgi.service.event
- org.osgi:org.osgi.service.http
- org.osgi:org.osgi.service.http.whiteboard
- org.osgi:org.osgi.service.log
- org.osgi:org.osgi.namespace.contract
- org.osgi:org.osgi.namespace.extender
- org.osgi:org.osgi.namespace.implementation
- org.osgi:org.osgi.namespace.service
- org.osgi:org.osgi.namespace.unresolvable
- org.osgi:org.osgi.annotation.versioning
- org.osgi:org.osgi.annotation.bundle
- org.osgi:org.osgi.service.component.annotations
- org.osgi:org.osgi.service.metatype.annotations

