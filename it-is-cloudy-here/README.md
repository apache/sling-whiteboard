Sling Cloud
====

This repository hosts a set of PoC modules, meant to show how a Sling instance can be developed in a cloud friendly way, with a remote
content store provided by storage services (e.g. Dropbox, Google Drive, Box, OneDrive, etc.).

Available modules:
1. [`org.apache.sling.remote.resourceprovider`](./org-apache-sling-remote-resourceprovider) - new API for exposing remote files and folders
as `Resources` in the Sling tree
2. [`org.apache.sling.remote.resourceprovider.dropbox`](./org-apache-sling-remote-resourceprovider-dropbox) - a Dropbox implementation of
the API from 1.
3. [`org.apache.sling.mini`](./org-apache-sling-mini) - builds a Sling feature providing most of the core bundles needed to build a web
application, with the exception of a `ResourceProvider` and a `ScriptEngine`
4. [`org.apache.sling.mini.demo`](./org-apache-sling-mini-demo) - builds a Sling web application, by extending the feature provided by
`org.apache.sling.mini`
5. [`org-apache-sling-mini-demo-content`](./org-apache-sling-mini-demo-content) - demo content to be deployed on Dropbox for
`org.apache.sling.mini.demo`
