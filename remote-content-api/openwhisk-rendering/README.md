openwhisk-rendering
============

This is a minimal HTTP proxy that renders JSON content acquired over the
Sling Remote Content API. 

The goal is to demonstrate that the API provides enough content in a single
request to render a Web page, using the `sling:resourceType` values to select
specific renderers at the OpenWhisk level.

To test this, run the sibling `sample-app` Sling instance, which is configured with
the Remote Content API, and deploy the code from this module to an 
[Apache OpenWhisk](http://openwhisk.apache.org/) service.

To do this, first setup the `wsk` command, either running the OpenWhisk runnable
jar locally or getting access to an OpenWhisk service.

If running this code in an OpenWhisk Cloud service, you need to make your Sling
instance available to it at a public URL, using `ngrok` or a similar tool.

Then run this:

    $ npm install # just once, of course
    $ export ORIGIN=<public URL of your Sling sample-app> # without trailing slash
    $ export ROOT_URL=<URL of the OpenWhisk action> #  need to deploy once to get it, see URL below
    $ zip -r action.zip package.json node_modules *.js && wsk action update hproxy action.zip --web true --kind nodejs:10 -p ORIGIN $ORIGIN -p ROOT_URL $ROOT_URL
    ok: created action hproxy
    
    $ export URL=$(wsk -i action get hproxy --url | grep http)

And then open $URL from a browser. 

To see what happened at the OpenWhisk level, or for troubleshooting you can use:

    $ wsk -i activation get --last
    $ wsk -i activation logs --last