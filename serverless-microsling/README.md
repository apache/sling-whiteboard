Serverless Microsling
===

This prototype, created for the [adaptTo() 2019 conference](https://adapt.to/2019/en/schedule/sling-and-serverless-best-friends-forever.html), implements a minimal Sling-like request processing pipeline in a serverless environment.

Its name comes from the [historical microsling project](https://grep.codeconsult.ch/2007/10/12/microsling-yet-another-cool-web-applications-framework/) from 2007, which served as the basis of today's Sling architecture and naming.

Status
----
Work in progress - stay tuned

How to run this on Apache OpenWhisk
---
Assuming `wsk` is setup as per the [OpenWhisk docs](openwhisk.apache.org), the
`install` script installs this code as an OpenWhisk action named `microsling`.

To see what happened after executing the action you can use:

    wsk -i activation get --last

and

    wsk -i activation logs --last