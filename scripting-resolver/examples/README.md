Examples
====

Because some code is worth a thousand words...

The examples provided by the project also serve as integration tests, therefore they should always work and be relatively up-to-date.
Two bundles showcase how the [`org.apache.sling.scripting.resolver`](../org-apache-sling-scripting-resolver/README.md) works and how you can
package your scripts into OSGi bundles:

  1. [`org.apache.sling.scripting.examplebundle`](./org-apache-sling-scripting-examplebundle):
    * provides the `org.apache.sling.scripting.examplebundle.hello` resource type
    * the resource type is provided in two versions (`1.0.0` and `2.0.0`)
    * it has a main script (`hello.html`) and some selector scripts
    * initial content is available at:
      1. `<sling>/content/srr/examples/hello.html` - automatically wired to the `org.apache.sling.scripting.examplebundle.hello/2.0.0` resource type
      2. `<sling>/content/srr/examples/hello-v1.html` - explicitly wired to the `org.apache.sling.scripting.examplebundle.hello/1.0.0` resource type
      3. `<sling>/content/srr/examples/hello-v2.html` - explicitly wired to the `org.apache.sling.scripting.examplebundle.hello/2.0.0` resource type

  2. [`org.apache.sling.scripting.examplebundle.hi`](./org-apache-sling-scripting-examplebundle.hi):
    * provides the `org.apache.sling.scripting.examplebundle.hi` resource type; it extends
    `org.apache.sling.scripting.examplebundle.hello;version=[1.0.0,2.0.0)` - the `extends` file will be used by the
    [`org.apache.sling.scripting.maven.plugin`](../org-apache-sling-scripting-maven-plugin) to correctly generate the
    `Require-Capabilities`
    * the resource type is provided in one version (`1.0.0`)
    * it has a selector script (`h.html`), essentially overriding the same selector script from
    `org.apache.sling.scripting.examplebundle.hello/1.0.0`
    * initial content is available at:
      1. `<sling>/content/srr/examples/hi.html` - automatically wired to the `org.apache.sling.scripting.examplebundle.hi/1.0.0` resource type
      2. `<sling>/content/srr/examples/hi-v1.html` - explicitly wired to the `org.apache.sling.scripting.examplebundle.hi/1.0.0` resource type

  3. [`org.apache.sling.scripting.examplebundle.precompiled`](./org-apache-sling-scripting-examplebundle-precompiled):
    * provides the `org.apache.sling.scripting.examplebundle.precompiled.hello` resource type
    * the resource type is provided in two versions (`1.0.0` and `2.0.0`)
    * it has a main script (`hello.html`) and some selector scripts
    * initial content is available at:
      1. `<sling>/content/srr/examples/precompiled-hello.html` - automatically wired to the `org.apache.sling.scripting.examplebundle.hello/2.0.0` resource type
      2. `<sling>/content/srr/examples/precompiled-hello-v1.html` - explicitly wired to the `org.apache.sling.scripting.examplebundle.hello/1.0.0` resource type
      3. `<sling>/content/srr/examples/precompiled-hello-v2.html` - explicitly wired to the `org.apache.sling.scripting.examplebundle.hello/2.0.0` resource type

The integration tests using these bundles can be found [here](../org-apache-sling-scripting-resolver/src/test/java/org/apache/sling/scripting/resolver/internal) and
you can find instructions to start a running sling instance with the provided examples [here](../org-apache-sling-scripting-resolver/#Example).
