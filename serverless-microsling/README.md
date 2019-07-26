Serverless Microsling
===

This prototype, created for the [adaptTo() 2019 conference](https://adapt.to/2019/en/schedule/sling-and-serverless-best-friends-forever.html), implements a minimal Sling-like request processing pipeline in a serverless environment.

Its name comes from the [historical microsling project](https://grep.codeconsult.ch/2007/10/12/microsling-yet-another-cool-web-applications-framework/) from 2007, which served as the basis of today's Sling architecture and naming.

Status
----
At [commit 38794912](https://github.com/apache/sling-whiteboard/commit/387949128e32557aac796da4543346e73288f49c), dynamic selection of renderers
implemented by independent OpenWhisk functions (in the same namespace) works. See _playing with the dynamic renderers selection_ below for how to experiment with that.

Prerequisite: Apache OpenWhisk
---
To run this prototype you'll need the OpenWhisk `wsk` command to be is setup as per 
the [OpenWhisk docs](http://openwhisk.apache.org).

A simple way to play with OpenWhisk is to use its [standalone runnable jar](https://github.com/apache/incubator-openwhisk/pull/4516). That's not released as I write
this but you can download a build from [Chetan's repository](https://github.com/chetanmeh/incubator-openwhisk/releases/tag/v0.10), which has been tested with this code.

Running the examples
---
Run the `install` script to install this code as an OpenWhisk action named `microsling`, along with a 
few rendering actions that demonstrate dynamic bindings of renderers.

The script outputs the URL at which the action is available:

    Microsling is available at at https://openwhisk.example.com/YOURNAME/default/microsling

Along with a set of URLs of test documents.

Playing with the dynamic renderers selection
---
The non-default renderers are separate OpenWhisk actions selected based on annotations prefixed
with `sling:`.

These action annotations are set by the `install` script but you can change them later as in these examples:

    # Vintage! switch to the `htm` extension for the `microsling/somedoc` resource type.
    # html will be handled by the default renderer
    wsk action update somedoc-html -a sling:extensions htm \
      -a sling:resourceType microsling/somedoc \
      -a sling:contentType text/html

    # More vintage! Use the `somedoc-html` renderer for all resource types, with the `htm` extension:
    wsk action update somedoc-html -a sling:extensions htm \
      -a sling:resourceType '*' \
      -a sling:contentType text/html

    # Back to normal for this `somedoc-html` rendering action
    wsk action update somedoc-html rendering-actions/somedoc-html.js \
      -a sling:resourceType microsling/somedoc \
      -a sling:contentType text/html -a sling:extensions html

To create new renderers see the examples in the [rendering-actions](./rendering-actions/) folder and how
they are setup by the [install](./install) script.

Troubleshooting
---
To see what happened after executing the action you can use:

    wsk -i activation get --last

Or `wsk activation list` to see what was executed and `wsk activation get <ID>` to get the 
data of a specific action activation.

The `wsk activation logs` command outputs just the logs.

See the [Apache OpenWhisk documentation](http://openwhisk.apache.org/) for more information.
