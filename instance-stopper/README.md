[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

# Apache Sling Instance Stopper

This module is part of the [Apache Sling](https://sling.apache.org) project.

The instance stopper allows an instance to the shut down as soon as it is considered ready.
The main scenario is part of the tooling for setting up a [CompositeNodeStore](https://jackrabbit.apache.org/oak/docs/nodestore/compositens.html).

The readiness check is delegated to the [Felix HealthChecks](https://github.com/apache/felix-dev/tree/master/healthcheck).

## Usage

Include the bundle in your deployment.
