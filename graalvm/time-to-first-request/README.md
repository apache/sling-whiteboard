time-to-first-request
====

An HTTP proxy written in NodeJS that starts our
native image container on demand when an HTTP request is received.

Used to measure the time-to-first-request on our native image.

To run this use:

    npm install
    node index.js
    
See [index.js](./index.js) for more details - a number of things (Docker image name, ports etc.) are hardcoded in there.
