# Microsling: the Sling request processing, reduced to the max

THIS CODE IS KEPT FOR HISTORICAL REASONS ONLY !

As it helped define the Sling design back in the times.

The goal of this prototype, created
[back in 2007](https://grep.codeconsult.ch/2007/10/12/microsling-yet-another-cool-web-applications-framework/)
was to demonstrate the Sling HTTP request processing in the simplest possible way, to help the
community converge on the goals and architecture of this 
module.

## How to build and run this

To build and run this use

    mvn clean package jetty:run
  
Which should say "Started SelectChannelConnector@0.0.0.0:8080" once
the build is done.  
  
Then, connect to http://localhost:8080/ which should return a page
saying "Microsling homepage" with instructions for playing with
this code.
