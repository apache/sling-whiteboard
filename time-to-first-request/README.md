time-to-first-request
====

This HTTP proxy, written in NodeJS, starts a Docker
container on demand when an HTTP request is received.

It can be used to measure the _time to first successful request_ on containers.

To run this use:

    npm install
    node index.js

With the default settings it says

    listening on port 9000, proxying to http://127.0.0.1:8080 with httpd:2.4.39-alpine on port 8080/80
    
Which means that an HTTP request on port 9000 starts the `httpd:2.4.39-alpine` Docker image and proxies the request to its 8080 port, noting the timings when doing so:

    Starting container: httpd:2.4.39-alpine(20.660ms)
    Waiting on http://127.0.0.1:8080//index.html (23.164ms)
    Time to wait for http://127.0.0.1:8080//index.html: (1.282s)
    Proxying / (1.282s)
    Done proxying (1.284s)

The `/80` port is the one that the container exposes.

The Docker image must be downloaded before running this, with `docker pull ...`.

The `--help` option explains how to override the above values from the command-line.
