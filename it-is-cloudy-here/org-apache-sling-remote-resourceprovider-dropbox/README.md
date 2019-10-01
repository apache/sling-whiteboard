Apache Sling Remote Resource Provider for Dropbox
====

The Apache Sling Remote Resource Provider for Dropbox provides a read-only implementation of the `RemoteStorageProvider`
API from [`org.apache.sling.remote.resourceprovider`](../org-apache-sling-remote-resourceprovider), allowing to expose a
Dropbox application folder as a tree of Sling resources.

To get started with this module perform the following steps:

1. Go to the Dropbox app console at [https://www.dropbox.com/developers/apps](https://www.dropbox.com/developers/apps)
2. Create a new app:
    1. Select the Dropbox API
    2. Select an "App folder" for the access you need
    3. Name your app
3. Configure an instance of the `DropboxStorageProvider` on your instance by using a generated access token
