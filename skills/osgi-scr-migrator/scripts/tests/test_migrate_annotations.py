#!/usr/bin/env python3
"""
Unit tests for migrate_annotations.py
"""

import sys
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from migrate_annotations import (
    AnnotationMigrator,
    Property,
    PropertyType,
    MigrationStats,
    migrate_file
)


class TestPropertyParsing(unittest.TestCase):
    """Test @Property annotation parsing."""

    def test_simple_string_property(self):
        """Test parsing simple string property."""
        content = '''
@Property(name = "service.vendor", value = "Apache Software Foundation")
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        migrator._collect_properties()

        self.assertEqual(len(migrator.component_properties), 1)
        prop = migrator.component_properties[0]
        self.assertEqual(prop.name, "service.vendor")
        self.assertEqual(prop.value, "Apache Software Foundation")
        self.assertEqual(prop.type, PropertyType.STRING)

    def test_boolean_property(self):
        """Test parsing boolean property."""
        content = '''
@Property(name = "enabled", boolValue = true)
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        migrator._collect_properties()

        self.assertEqual(len(migrator.component_properties), 1)
        prop = migrator.component_properties[0]
        self.assertEqual(prop.name, "enabled")
        self.assertEqual(prop.value, "true")
        self.assertEqual(prop.type, PropertyType.BOOLEAN)

    def test_integer_property(self):
        """Test parsing integer property."""
        content = '''
@Property(name = "service.ranking", intValue = 100)
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        migrator._collect_properties()

        self.assertEqual(len(migrator.component_properties), 1)
        prop = migrator.component_properties[0]
        self.assertEqual(prop.name, "service.ranking")
        self.assertEqual(prop.value, "100")
        self.assertEqual(prop.type, PropertyType.INTEGER)

    def test_array_property(self):
        """Test parsing array property."""
        content = '''
@Property(name = "paths", value = {"/path1", "/path2", "/path3"})
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        migrator._collect_properties()

        self.assertEqual(len(migrator.component_properties), 1)
        prop = migrator.component_properties[0]
        self.assertEqual(prop.name, "paths")
        self.assertEqual(len(prop.values), 3)
        self.assertEqual(prop.values, ["/path1", "/path2", "/path3"])

    def test_property_with_label(self):
        """Test parsing property with label and description."""
        content = '''
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout in seconds")
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        migrator._collect_properties()

        self.assertEqual(len(migrator.component_properties), 1)
        prop = migrator.component_properties[0]
        self.assertEqual(prop.name, "timeout")
        self.assertEqual(prop.label, "Timeout")
        self.assertEqual(prop.description, "Request timeout in seconds")
        self.assertTrue(prop.is_configurable)


class TestPropertyConversion(unittest.TestCase):
    """Test property conversion to OSGi R6/R7 format."""

    def test_simple_property_conversion(self):
        """Test converting simple property to component format."""
        prop = Property(name="service.vendor", value="Apache Software Foundation")
        result = prop.to_component_property()

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], '"service.vendor=Apache Software Foundation"')

    def test_integer_property_conversion(self):
        """Test converting integer property with type specification."""
        prop = Property(name="service.ranking", value="100", type=PropertyType.INTEGER)
        result = prop.to_component_property()

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], '"service.ranking:Integer=100"')

    def test_boolean_property_conversion(self):
        """Test converting boolean property with type specification."""
        prop = Property(name="enabled", value="true", type=PropertyType.BOOLEAN)
        result = prop.to_component_property()

        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], '"enabled:Boolean=true"')

    def test_array_property_conversion(self):
        """Test converting array property."""
        prop = Property(name="paths", values=["/path1", "/path2"])
        result = prop.to_component_property()

        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], '"paths=/path1"')
        self.assertEqual(result[1], '"paths=/path2"')

    def test_metatype_attribute_conversion(self):
        """Test converting property to metatype attribute."""
        prop = Property(
            name="my.timeout",
            value="30",
            type=PropertyType.INTEGER,
            label="Timeout",
            description="Request timeout"
        )
        result = prop.to_metatype_attribute()

        self.assertIn('@AttributeDefinition', result)
        self.assertIn('name = "Timeout"', result)
        self.assertIn('description = "Request timeout"', result)
        self.assertIn('int my_timeout()', result)
        self.assertIn('default 30', result)


class TestSlingServletMigration(unittest.TestCase):
    """Test @SlingServlet migration."""

    def test_simple_servlet_migration(self):
        """Test migrating simple Sling servlet to Sling Servlets Annotations."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.sling.SlingServlet;

@SlingServlet(paths = "/bin/myservlet", methods = "GET")
public class MyServlet extends SlingAllMethodsServlet {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@Component(service = Servlet.class)', new_content)
        self.assertIn('@SlingServletPaths(value = "/bin/myservlet")', new_content)
        self.assertNotIn('org.apache.felix.scr.annotations.sling.SlingServlet', new_content)
        self.assertIn('org.apache.sling.servlets.annotations.SlingServletPaths', new_content)

    def test_servlet_with_resource_types(self):
        """Test migrating servlet with resource types."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.sling.SlingServlet;

@SlingServlet(
    resourceTypes = "my/resource/type",
    selectors = "print",
    extensions = {"html", "pdf"}
)
public class MyServlet extends SlingAllMethodsServlet {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@Component(service = Servlet.class)', new_content)
        self.assertIn('@SlingServletResourceTypes', new_content)
        self.assertIn('resourceTypes = "my/resource/type"', new_content)
        self.assertIn('selectors = "print"', new_content)
        self.assertIn('extensions = {"html", "pdf"}', new_content)
        self.assertNotIn('org.apache.felix.scr.annotations.sling.SlingServlet', new_content)
        self.assertIn('org.apache.sling.servlets.annotations.SlingServletResourceTypes', new_content)

    def test_sling_filter_migration(self):
        """Test migrating Sling filter to Sling Servlets Annotations."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;

@SlingFilter(
    scope = SlingFilterScope.REQUEST,
    order = -700,
    pattern = "/content/.*"
)
public class MyFilter implements Filter {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@Component(service = Filter.class)', new_content)
        self.assertIn('@SlingServletFilter', new_content)
        self.assertIn('scope = SlingServletFilterScope.REQUEST', new_content)
        self.assertIn('order = -700', new_content)
        self.assertIn('pattern = "/content/.*"', new_content)
        self.assertNotIn('org.apache.felix.scr.annotations.sling.SlingFilter', new_content)
        self.assertIn('org.apache.sling.servlets.annotations.SlingServletFilter', new_content)
        self.assertIn('org.apache.sling.servlets.annotations.SlingServletFilterScope', new_content)


class TestComponentMigration(unittest.TestCase):
    """Test @Component migration."""

    def test_component_with_service_merge(self):
        """Test merging @Service into @Component."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(value = MyInterface.class)
public class MyService implements MyInterface {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@Component(', new_content)
        self.assertIn('service = MyInterface.class', new_content)
        self.assertNotIn('@Service', new_content)
        self.assertIn('org.osgi.service.component.annotations.Component', new_content)

    def test_component_with_properties(self):
        """Test component with properties (now uses property type annotations)."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component(immediate = true)
@Service
@Property(name = "service.vendor", value = "Apache Software Foundation")
@Property(name = "service.ranking", intValue = 100)
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('immediate = true', new_content)
        # These now use property type annotations instead of property strings
        self.assertIn('@ServiceVendor("Apache Software Foundation")', new_content)
        self.assertIn('@ServiceRanking(100)', new_content)
        self.assertNotIn('@Property', new_content)
        # Verify imports
        self.assertIn('org.osgi.service.component.propertytypes.ServiceVendor', new_content)
        self.assertIn('org.osgi.service.component.propertytypes.ServiceRanking', new_content)


class TestReferenceMigration(unittest.TestCase):
    """Test @Reference migration."""

    def test_cardinality_migration(self):
        """Test reference cardinality migration."""
        test_cases = [
            ('OPTIONAL_UNARY', 'OPTIONAL'),
            ('MANDATORY_UNARY', 'MANDATORY'),
            ('OPTIONAL_MULTIPLE', 'MULTIPLE'),
            ('MANDATORY_MULTIPLE', 'AT_LEAST_ONE'),
        ]

        for old, new in test_cases:
            content = f'''
@Reference(cardinality = ReferenceCardinality.{old})
private MyService myService;
'''
            stats = MigrationStats()
            migrator = AnnotationMigrator(content, Path("test.java"), stats)
            new_content, changed = migrator.migrate()

            self.assertTrue(changed)
            self.assertIn(f'ReferenceCardinality.{new}', new_content)
            self.assertNotIn(f'ReferenceCardinality.{old}', new_content)


class TestMetatypeMigration(unittest.TestCase):
    """Test metatype configuration generation."""

    def test_metatype_config_generation(self):
        """Test generating metatype configuration interface."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component(metatype = true)
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout")
@Property(name = "retries", intValue = 3, label = "Retries", description = "Number of retries")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@Designate(ocd = MyService.Config.class)', new_content)
        self.assertIn('@ObjectClassDefinition', new_content)
        self.assertIn('public @interface Config {', new_content)
        self.assertIn('int timeout()', new_content)
        self.assertIn('int retries()', new_content)
        self.assertIn('org.osgi.service.metatype.annotations', new_content)

    def test_metatype_config_with_component_label_description(self):
        """Test that @Component label and description are preserved in @ObjectClassDefinition."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component(metatype = true, label = "My Service", description = "This is my service")
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout in seconds")
@Property(name = "enabled", boolValue = true, label = "Enable Feature", description = "Enable this feature")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Check that @Component label/description are used in @ObjectClassDefinition
        self.assertIn('@ObjectClassDefinition(name = "My Service", description = "This is my service")', new_content)
        # Check that @Property label/description are used in @AttributeDefinition
        self.assertIn('@AttributeDefinition(name = "Timeout", description = "Request timeout in seconds")', new_content)
        self.assertIn('@AttributeDefinition(name = "Enable Feature", description = "Enable this feature")', new_content)
        self.assertIn('int timeout()', new_content)
        self.assertIn('boolean enabled()', new_content)

    def test_component_property_types_activate_method(self):
        """Test that @Activate methods using Map are converted to use Config type."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import java.util.Map;

@Component(metatype = true)
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout")
public class MyService {

    @Activate
    protected void activate(Map<String, Object> properties) {
        // activation code
    }
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Check that Map<String, Object> was replaced with Config
        self.assertNotIn('Map<String, Object>', new_content)
        # Check that parameter name is preserved (was 'properties')
        self.assertIn('Config properties', new_content)
        # Check that the Config interface was generated
        self.assertIn('public @interface Config', new_content)
        self.assertIn('@Designate(ocd = MyService.Config.class)', new_content)


class TestFullMigration(unittest.TestCase):
    """Test full file migration."""

    def test_complete_file_migration(self):
        """Test migrating a complete file."""
        with TemporaryDirectory() as tmpdir:
            tmpdir_path = Path(tmpdir)
            test_file = tmpdir_path / "TestService.java"

            content = '''package com.example;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

@Component(immediate = true)
@Service(value = MyInterface.class)
@Property(name = "service.vendor", value = "Apache Software Foundation")
public class TestService implements MyInterface {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private OtherService otherService;

    @Activate
    protected void activate() {
        // activation code
    }

    @Deactivate
    protected void deactivate() {
        // deactivation code
    }
}
'''
            test_file.write_text(content)

            stats = MigrationStats()
            result = migrate_file(test_file, dry_run=False, stats=stats)

            self.assertTrue(result)
            self.assertEqual(stats.files_changed, 1)
            self.assertGreater(stats.components_migrated, 0)
            self.assertGreater(stats.services_merged, 0)

            # Check migrated content
            migrated_content = test_file.read_text()
            self.assertIn('org.osgi.service.component.annotations', migrated_content)
            self.assertNotIn('org.apache.felix.scr.annotations', migrated_content)
            self.assertIn('service = MyInterface.class', migrated_content)
            self.assertIn('ReferenceCardinality.OPTIONAL', migrated_content)


class TestPropertyTypeAnnotations(unittest.TestCase):
    """Test Component Property Type annotations."""

    def test_service_ranking_property_type(self):
        """Test that service.ranking uses @ServiceRanking annotation."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
@Property(name = "service.ranking", intValue = 100)
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@ServiceRanking(100)', new_content)
        self.assertNotIn('service.ranking', new_content)
        self.assertIn('org.osgi.service.component.propertytypes.ServiceRanking', new_content)
        self.assertEqual(stats.property_type_annotations_added, 1)

    def test_service_description_property_type(self):
        """Test that service.description uses @ServiceDescription annotation."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
@Property(name = "service.description", value = "My Service Description")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@ServiceDescription("My Service Description")', new_content)
        self.assertNotIn('service.description', new_content)
        self.assertIn('org.osgi.service.component.propertytypes.ServiceDescription', new_content)
        self.assertEqual(stats.property_type_annotations_added, 1)

    def test_service_vendor_property_type(self):
        """Test that service.vendor uses @ServiceVendor annotation."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
@Property(name = "service.vendor", value = "Apache Software Foundation")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@ServiceVendor("Apache Software Foundation")', new_content)
        self.assertNotIn('service.vendor', new_content)
        self.assertIn('org.osgi.service.component.propertytypes.ServiceVendor', new_content)
        self.assertEqual(stats.property_type_annotations_added, 1)

    def test_multiple_property_type_annotations(self):
        """Test multiple property type annotations together."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
@Property(name = "service.vendor", value = "Apache Software Foundation")
@Property(name = "service.ranking", intValue = 100)
@Property(name = "service.description", value = "My Service")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        self.assertIn('@ServiceVendor("Apache Software Foundation")', new_content)
        self.assertIn('@ServiceRanking(100)', new_content)
        self.assertIn('@ServiceDescription("My Service")', new_content)
        self.assertEqual(stats.property_type_annotations_added, 3)

    def test_mixed_properties_and_property_types(self):
        """Test that regular properties and property type annotations work together."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service
@Property(name = "service.ranking", intValue = 100)
@Property(name = "my.custom.property", value = "custom")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Property type annotation
        self.assertIn('@ServiceRanking(100)', new_content)
        self.assertIn('org.osgi.service.component.propertytypes.ServiceRanking', new_content)
        # Regular property
        self.assertIn('"my.custom.property=custom"', new_content)
        self.assertEqual(stats.property_type_annotations_added, 1)


class TestPropertyTypeAnnotationEdgeCases(unittest.TestCase):
    """Test edge cases for Component Property Type annotations."""

    def test_service_ranking_wrong_type_string_value(self):
        """Test that service.ranking with string value falls back to property string."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component
@Property(name = "service.ranking", value = "100")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Should fall back to property string, NOT disappear
        self.assertIn('"service.ranking=100"', new_content)
        self.assertNotIn('@ServiceRanking', new_content)
        # Should have warning
        self.assertGreaterEqual(len(stats.warnings), 1)
        warning_text = ' '.join(stats.warnings)
        self.assertIn('has type', warning_text.lower())

    def test_service_description_wrong_type_int_value(self):
        """Test that service.description with intValue falls back to property string."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component
@Property(name = "service.description", intValue = 123)
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Should fall back to property string
        self.assertIn('"service.description:Integer=123"', new_content)
        self.assertNotIn('@ServiceDescription', new_content)
        # Should have warning
        self.assertGreaterEqual(len(stats.warnings), 1)

    def test_service_vendor_wrong_type_bool_value(self):
        """Test that service.vendor with boolValue falls back to property string."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component
@Property(name = "service.vendor", boolValue = true)
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Should fall back to property string
        self.assertIn('"service.vendor:Boolean=true"', new_content)
        self.assertNotIn('@ServiceVendor', new_content)

    def test_mixed_correct_and_wrong_types(self):
        """Test that some properties with correct types and some with wrong types are handled correctly."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component
@Property(name = "service.ranking", intValue = 100)
@Property(name = "service.vendor", value = "Apache Software Foundation")
@Property(name = "my.custom", value = "custom")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Correct types should use annotations
        self.assertIn('@ServiceRanking(100)', new_content)
        self.assertIn('@ServiceVendor("Apache Software Foundation")', new_content)
        # Custom property should use property string
        self.assertIn('"my.custom=custom"', new_content)

    def test_property_type_with_label_not_annotation(self):
        """Test that service.ranking with label doesn't use @ServiceRanking (becomes configurable)."""
        content = '''package com.example;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

@Component(metatype = true)
@Property(name = "service.ranking", intValue = 100, label = "Service Ranking", description = "The ranking")
public class MyService {
}
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        new_content, changed = migrator.migrate()

        self.assertTrue(changed)
        # Should NOT use @ServiceRanking because it has label (configurable)
        self.assertNotIn('@ServiceRanking', new_content)
        # Should generate metatype config
        self.assertIn('@Designate', new_content)
        self.assertIn('service_ranking()', new_content)


class TestMigrationStats(unittest.TestCase):
    """Test migration statistics tracking."""

    def test_stats_tracking(self):
        """Test that statistics are properly tracked."""
        content = '''
@Component
@Service
@Property(name = "prop1", value = "value1")
@Reference
private Service service;
'''
        stats = MigrationStats()
        migrator = AnnotationMigrator(content, Path("test.java"), stats)
        migrator.migrate()

        self.assertGreater(stats.components_migrated, 0)
        self.assertGreater(stats.services_merged, 0)
        self.assertGreater(stats.properties_migrated, 0)


def run_tests():
    """Run all tests."""
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(sys.modules[__name__])
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    sys.exit(run_tests())
