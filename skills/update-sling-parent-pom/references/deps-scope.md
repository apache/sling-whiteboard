# Explicitly set dependency scopes

- set scope to `test` for dependencies that are only used in tests
- otherwise use scope `provided` to prevent transitive dependencies from being included in the classpath
