[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

# Apache Sling Testing OSGi Unit

This module is part of the [Apache Sling](https://sling.apache.org) project.

OSGi Unit is a JUnit Jupiter extension that, for test methods
annotated with `@OSGiSupport`, or test methods in classes annotated
with `@OSGiSupport`:

- creates a test-bundle containing the test class and only those  
  classes in the module that are statically required by the test
  class
- resolves the test-bundle with all bundles on the class path
- bootstraps an OSGi framework and installs the resolved bundles
- runs the test method of the test class within the context of
  the OSGi framework (i.e. the test class is loaded by the
  test-bundle's class loader)
- Framework, Bundle (test-bundle) and BundleContext (test-bundle)
  objects can be injected into the test method 
- test method parameters annotated with `@Service` are retrieved
  from the service registry and injected (or null is injected in
  their absence), arrays, Iterables, Collections and Lists are
  supported

## Example

```
class SampleTest {

    @Component(service = AdHoc.class)
    public static class AdHoc {
        // ... implementation ...
    } 
    

    @OSGiSupport
    void testMyService(Bundle bundle, @Service AdHoc adHoc, @Service ServiceComponentRuntime scr) {
        // ... assertions ...
    } 
}
```

## Known Limitations

Due to the fact that the test-class and test-instance are processed outside the OSGi environment,
`org.junit.jupiter.api.extension.ParameterResolver` implementations returning objects that are **not
exported by the system-bundle** in a running OSGi framework, cannot currently be injected.

Known injections that are currently impossible due to this limitation are
- `org.junit.jupiter.api.TestInfo`
- `org.junit.jupiter.api.RepetitionInfo`
