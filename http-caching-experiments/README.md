This folder is meant to experiment with HTTP caching behavior for
https://issues.apache.org/jira/browse/SLING-9655
which is meant to implement caching of GraphQL query results at the HTTP level.

To start the caching service container image use

    docker build -t cache .
    docker run -it -p 8080:80 cache

And you can then experiment with requests like the following to see
what's being cached or not:

    export P=max-age
    for i in 1 2 3 4 5
    do
      curl -D - "http://localhost:8080/${P}.lua"
      sleep 2
    done

Which should show the same cached content ("max-age=5 at <timestamp>") for 5
seconds and then another timestamp for 5 seconds, etc.

Using the `private` path instead of `max-age` shows no caching.

TODO: add examples of request revalidation.