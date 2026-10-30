#!/usr/bin/env python3
"""
Unit tests for constructor injection migration script.
"""

import unittest
import sys
from pathlib import Path

# Add parent directory to path to import the migration script
sys.path.insert(0, str(Path(__file__).parent.parent))

from migrate_constructor_injection import (
    ConstructorInjectionMigrator,
    ConstructorInjectionStats,
    Reference,
    ReferenceCardinality,
    ReferencePolicy
)


class TestReferenceEligibility(unittest.TestCase):
    """Test reference eligibility for constructor injection."""

    def test_mandatory_static_unary_is_eligible(self):
        """Mandatory static unary reference should be eligible."""
        ref = Reference(
            field_name="service",
            field_type="MyService",
            line_idx=10,
            annotation_start_idx=8,
            annotation_end_idx=9,
            cardinality=None,  # Default is mandatory
            policy=None,  # Default is static
        )
        self.assertTrue(ref.is_eligible_for_constructor_injection())

    def test_optional_reference_not_eligible(self):
        """Optional reference should not be eligible."""
        ref = Reference(
            field_name="service",
            field_type="MyService",
            line_idx=10,
            annotation_start_idx=8,
            annotation_end_idx=9,
            cardinality=ReferenceCardinality.OPTIONAL,
        )
        self.assertFalse(ref.is_eligible_for_constructor_injection())

    def test_dynamic_reference_not_eligible(self):
        """Dynamic reference should not be eligible."""
        ref = Reference(
            field_name="service",
            field_type="MyService",
            line_idx=10,
            annotation_start_idx=8,
            annotation_end_idx=9,
            policy=ReferencePolicy.DYNAMIC,
        )
        self.assertFalse(ref.is_eligible_for_constructor_injection())

    def test_multiple_reference_not_eligible(self):
        """Multiple reference should not be eligible."""
        ref = Reference(
            field_name="services",
            field_type="MyService",
            line_idx=10,
            annotation_start_idx=8,
            annotation_end_idx=9,
            cardinality=ReferenceCardinality.MULTIPLE,
        )
        self.assertFalse(ref.is_eligible_for_constructor_injection())

    def test_collection_reference_not_eligible(self):
        """Collection reference should not be eligible."""
        ref = Reference(
            field_name="services",
            field_type="List<MyService>",
            line_idx=10,
            annotation_start_idx=8,
            annotation_end_idx=9,
            is_collection=True,
        )
        self.assertFalse(ref.is_eligible_for_constructor_injection())

    def test_reference_with_bind_method_not_eligible(self):
        """Reference with bind method should not be eligible."""
        ref = Reference(
            field_name="service",
            field_type="MyService",
            line_idx=10,
            annotation_start_idx=8,
            annotation_end_idx=9,
            has_bind_method=True,
        )
        self.assertFalse(ref.is_eligible_for_constructor_injection())


class TestConfigMigration(unittest.TestCase):
    """Test configuration parameter migration to constructor."""

    def test_simple_config_migration(self):
        """Test migrating simple config parameter to constructor."""
        content = """package com.example;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        int timeout() default 30;
    }

    private int timeout;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
    }

    public void doSomething() {
        // Use timeout
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn("@Activate", result)
        self.assertIn("public MyService(", result)
        self.assertIn("Config config", result)
        self.assertIn("final int timeout", result)
        self.assertEqual(stats.configs_converted, 1)
        self.assertEqual(stats.fields_made_final, 1)

    def test_config_with_multiple_fields(self):
        """Test config migration with multiple field assignments."""
        content = """package com.example;

@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    public @interface Config {
        int timeout() default 30;
        int maxRetries() default 3;
        String endpoint() default "http://example.com";
    }

    private int timeout;
    private int maxRetries;
    private String endpoint;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
        this.maxRetries = config.maxRetries();
        this.endpoint = config.endpoint();
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn("public MyService(", result)
        self.assertIn("Config config", result)
        self.assertIn("this.timeout = config.timeout()", result)
        self.assertIn("this.maxRetries = config.maxRetries()", result)
        self.assertIn("this.endpoint = config.endpoint()", result)
        self.assertEqual(stats.fields_made_final, 3)


class TestReferenceMigration(unittest.TestCase):
    """Test reference migration to constructor."""

    def test_single_mandatory_reference(self):
        """Test migrating single mandatory reference to constructor."""
        content = """package com.example;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = MyService.class)
public class MyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Activate
    private void activate() {
        // Nothing here
    }

    public void doSomething() {
        // Use resolverFactory
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn("public MyService(", result)
        self.assertIn("@Reference ResourceResolverFactory resolverFactory", result)
        self.assertIn("final ResourceResolverFactory resolverFactory", result)
        self.assertIn("this.resolverFactory = resolverFactory;", result)
        self.assertEqual(stats.references_converted, 1)
        self.assertEqual(stats.fields_made_final, 1)

    def test_multiple_mandatory_references(self):
        """Test migrating multiple mandatory references."""
        content = """package com.example;

@Component(service = MyService.class)
public class MyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    @Activate
    private void activate() {
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn("@Reference ResourceResolverFactory resolverFactory", result)
        self.assertIn("@Reference SlingRepository repository", result)
        self.assertIn("this.resolverFactory = resolverFactory;", result)
        self.assertIn("this.repository = repository;", result)
        self.assertEqual(stats.references_converted, 2)

    def test_optional_reference_not_migrated(self):
        """Test that optional references are not migrated."""
        content = """package com.example;

@Component(service = MyService.class)
public class MyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;

    @Activate
    private void activate() {
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        # Only resolverFactory should be migrated
        self.assertIn("@Reference ResourceResolverFactory resolverFactory", result)
        # configAdmin should stay as field injection
        self.assertIn("@Reference(cardinality = ReferenceCardinality.OPTIONAL)", result)
        self.assertIn("private ConfigurationAdmin configAdmin", result)
        self.assertEqual(stats.references_converted, 1)

    def test_reference_with_target_attribute(self):
        """Test reference with target attribute is preserved."""
        content = """package com.example;

@Component(service = MyService.class)
public class MyService {

    @Reference(target = "(component.name=my.service)")
    private MyService service;

    @Activate
    private void activate() {
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@Reference(target = "(component.name=my.service)")', result)
        self.assertEqual(stats.references_converted, 1)


class TestCombinedMigration(unittest.TestCase):
    """Test combined config and reference migration."""

    def test_config_and_references_combined(self):
        """Test migrating both config and references together."""
        content = """package com.example;

@Component(service = MyService.class)
@Designate(ocd = MyService.Config.class)
public class MyService {

    public @interface Config {
        int timeout() default 30;
    }

    private int timeout;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
    }

    public void doSomething() {
        // Use services
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        # Config parameter should be first
        self.assertIn("public MyService(\n        Config config,", result)
        # Followed by mandatory references
        self.assertIn("@Reference ResourceResolverFactory resolverFactory", result)
        self.assertIn("@Reference SlingRepository repository", result)
        # Optional reference should stay as field
        self.assertIn("@Reference(cardinality = ReferenceCardinality.OPTIONAL)", result)
        self.assertIn("private ConfigurationAdmin configAdmin", result)

        self.assertEqual(stats.configs_converted, 1)
        self.assertEqual(stats.references_converted, 2)

    def test_constructor_parameter_order(self):
        """Test that constructor parameters are in correct order: config first, then references."""
        content = """package com.example;

@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    public @interface Config {
        String name() default "test";
    }

    private String name;

    @Reference
    private ServiceA serviceA;

    @Reference
    private ServiceB serviceB;

    @Activate
    private void activate(Config config) {
        this.name = config.name();
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)

        # Find constructor
        lines = result.split('\n')
        constructor_start = None
        for i, line in enumerate(lines):
            if 'public MyService(' in line:
                constructor_start = i
                break

        self.assertIsNotNone(constructor_start)

        # Check order: Config should be first parameter
        found_config = False
        found_servicea = False
        found_serviceb = False

        for i in range(constructor_start, constructor_start + 10):
            if i >= len(lines):
                break
            line = lines[i]
            if 'Config' in line and not found_config:
                found_config = True
                config_pos = i
            elif 'ServiceA' in line and not found_servicea:
                found_servicea = True
                servicea_pos = i
            elif 'ServiceB' in line and not found_serviceb:
                found_serviceb = True
                serviceb_pos = i

        self.assertTrue(found_config)
        self.assertTrue(found_servicea)
        self.assertTrue(found_serviceb)
        self.assertLess(config_pos, servicea_pos, "Config should come before ServiceA")
        self.assertLess(servicea_pos, serviceb_pos, "ServiceA should come before ServiceB")


class TestEdgeCases(unittest.TestCase):
    """Test edge cases and error conditions."""

    def test_no_component_annotation(self):
        """Test file without @Component is skipped."""
        content = """package com.example;

public class NotAComponent {
    public void doSomething() {
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("NotAComponent.java"), stats)
        result, changed = migrator.migrate()

        self.assertFalse(changed)
        self.assertEqual(len(stats.skipped), 1)

    def test_no_activate_method(self):
        """Test component without @Activate is skipped."""
        content = """package com.example;

@Component(service = MyService.class)
public class MyService {
    public void doSomething() {
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertFalse(changed)
        self.assertEqual(len(stats.skipped), 1)

    def test_activate_without_config_or_eligible_refs(self):
        """Test @Activate without config or eligible references is skipped."""
        content = """package com.example;

@Component(service = MyService.class)
public class MyService {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;

    @Activate
    private void activate() {
        // Nothing to migrate
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertFalse(changed)
        self.assertEqual(len(stats.skipped), 1)

    def test_already_using_constructor_injection(self):
        """Test component already using constructor injection is skipped."""
        content = """package com.example;

@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    public @interface Config {
        int timeout() default 30;
    }

    private final int timeout;
    private final ResourceResolverFactory resolverFactory;

    @Activate
    public MyService(
        Config config,
        @Reference ResourceResolverFactory resolverFactory
    ) {
        this.timeout = config.timeout();
        this.resolverFactory = resolverFactory;
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        # This should be skipped since @Activate is already on constructor
        # The script looks for @Activate followed by a method signature
        # Constructor signatures are different, so this should be skipped
        self.assertFalse(changed)


class TestFinalModifier(unittest.TestCase):
    """Test final modifier addition."""

    def test_fields_become_final(self):
        """Test that fields are made final when migrated."""
        content = """package com.example;

@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    public @interface Config {
        int timeout() default 30;
    }

    private int timeout;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn("private final int timeout", result)
        self.assertIn("private final ResourceResolverFactory resolverFactory", result)
        # At least 2 fields made final (timeout and resolverFactory)
        self.assertGreaterEqual(stats.fields_made_final, 2)

    def test_already_final_fields_unchanged(self):
        """Test that already final fields are not double-modified."""
        content = """package com.example;

@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    public @interface Config {
        int timeout() default 30;
    }

    private final int timeout;

    @Reference
    private final ResourceResolverFactory resolverFactory;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
    }
}
"""
        stats = ConstructorInjectionStats()
        migrator = ConstructorInjectionMigrator(content, Path("MyService.java"), stats)
        result, changed = migrator.migrate()

        self.assertTrue(changed)
        # Should not have "final final"
        self.assertNotIn("final final", result)


def run_tests():
    """Run all tests."""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()

    # Add all test classes
    suite.addTests(loader.loadTestsFromTestCase(TestReferenceEligibility))
    suite.addTests(loader.loadTestsFromTestCase(TestConfigMigration))
    suite.addTests(loader.loadTestsFromTestCase(TestReferenceMigration))
    suite.addTests(loader.loadTestsFromTestCase(TestCombinedMigration))
    suite.addTests(loader.loadTestsFromTestCase(TestEdgeCases))
    suite.addTests(loader.loadTestsFromTestCase(TestFinalModifier))

    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    sys.exit(run_tests())
