# Distribution Service

## Goals

* Build a multi-tenant service that exposes distribution queues via a REST API
* Build Sling Content Distribution (SCD) agents that leverage the service

Ideally the service would be implemented by repurposing and refactoring Sling Content Distribution Journal bundles.

## Non goals

* It's not an immediate goal to support consumers other than SCD
