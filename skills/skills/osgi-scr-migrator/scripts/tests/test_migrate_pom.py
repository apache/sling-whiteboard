#!/usr/bin/env python3
"""
Unit tests for migrate_pom.py
"""

import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from tempfile import TemporaryDirectory

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from migrate_pom import PomMigrator, PomStats, migrate_pom, detect_metatype_usage


class TestPomMigration(unittest.TestCase):
    """Test POM file migration."""

    def create_test_pom(self, content: str) -> Path:
        """Create a temporary POM file for testing."""
        tmpdir = TemporaryDirectory()
        self.addCleanup(tmpdir.cleanup)
        pom_path = Path(tmpdir.name) / "pom.xml"
        pom_path.write_text(content)
        return pom_path

    def test_remove_scr_plugin(self):
        """Test removing maven-scr-plugin."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-scr-plugin</artifactId>
                <version>1.26.0</version>
            </plugin>
        </plugins>
    </build>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrator = PomMigrator(pom_path, stats)
        migrator.load()
        migrator.migrate()
        migrator.save()  # Save changes to file before re-parsing

        self.assertTrue(migrator.changes_made)
        self.assertEqual(stats.scr_plugins_removed, 1)

        # Verify plugin was removed
        tree = ET.parse(pom_path)
        root = tree.getroot()
        namespace = root.tag.split('}')[0] + '}' if root.tag.startswith('{') else ''

        build = root.find(f'{namespace}build')
        plugins = build.find(f'{namespace}plugins')
        for plugin in plugins.findall(f'{namespace}plugin'):
            artifact_id = plugin.find(f'{namespace}artifactId')
            if artifact_id is not None:
                self.assertNotEqual(artifact_id.text, 'maven-scr-plugin')

    def test_remove_scr_dependency(self):
        """Test removing felix.scr.annotations dependency."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.felix</groupId>
            <artifactId>org.apache.felix.scr.annotations</artifactId>
            <version>1.12.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrator = PomMigrator(pom_path, stats)
        migrator.load()
        migrator.migrate()

        self.assertTrue(migrator.changes_made)
        self.assertEqual(stats.scr_deps_removed, 1)

    def test_add_osgi_annotations(self):
        """Test adding OSGi component annotations dependency."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.felix</groupId>
            <artifactId>org.apache.felix.scr.annotations</artifactId>
            <version>1.12.0</version>
        </dependency>
    </dependencies>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrate_pom(pom_path, dry_run=False, stats=stats)

        self.assertEqual(stats.osgi_deps_added, 2)

        # Verify dependencies were added
        content = pom_path.read_text()
        self.assertIn('org.osgi.service.component.annotations', content)
        self.assertIn('org.osgi.service.component</artifactId>', content)
        self.assertNotIn('felix.scr.annotations', content)

    def test_add_metatype_dependency(self):
        """Test adding metatype annotations dependency."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <dependencies/>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrate_pom(pom_path, dry_run=False, stats=stats, add_metatype=True)

        self.assertEqual(stats.metatype_deps_added, 1)

        # Verify dependency was added
        content = pom_path.read_text()
        self.assertIn('org.osgi.service.metatype.annotations', content)

    def test_update_bundle_plugin(self):
        """Test updating maven-bundle-plugin."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <version>5.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrate_pom(pom_path, dry_run=False, stats=stats)

        # Verify extensions was added
        content = pom_path.read_text()
        self.assertIn('<extensions>true</extensions>', content)

    def test_update_bnd_maven_plugin(self):
        """Test updating bnd-maven-plugin."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <build>
        <plugins>
            <plugin>
                <groupId>biz.aQute.bnd</groupId>
                <artifactId>bnd-maven-plugin</artifactId>
                <version>6.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrate_pom(pom_path, dry_run=False, stats=stats)

        # Verify extensions was added
        content = pom_path.read_text()
        self.assertIn('<extensions>true</extensions>', content)
        self.assertIn('bnd-maven-plugin', content)

    def test_version_detection(self):
        """Test OSGi version detection from existing dependencies."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.core</artifactId>
            <version>7.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.felix</groupId>
            <artifactId>org.apache.felix.scr.annotations</artifactId>
            <version>1.12.0</version>
        </dependency>
    </dependencies>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrator = PomMigrator(pom_path, stats, detect_versions=True)
        migrator.load()
        migrator.migrate()

        # Should detect OSGi 7 and use compatible versions
        self.assertEqual(migrator.osgi_version, "1.5.1")
        self.assertEqual(migrator.metatype_version, "1.4.1")

    def test_dry_run(self):
        """Test dry-run mode doesn't modify files."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.felix</groupId>
            <artifactId>org.apache.felix.scr.annotations</artifactId>
            <version>1.12.0</version>
        </dependency>
    </dependencies>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        original_content = pom_path.read_text()

        stats = PomStats()
        migrate_pom(pom_path, dry_run=True, stats=stats)

        # File should not be modified
        self.assertEqual(pom_path.read_text(), original_content)

    def test_complete_migration(self):
        """Test complete POM migration."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test-bundle</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.apache.felix</groupId>
            <artifactId>org.apache.felix.scr.annotations</artifactId>
            <version>1.12.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-scr-plugin</artifactId>
                <version>1.26.0</version>
                <executions>
                    <execution>
                        <id>generate-scr-scrdescriptor</id>
                        <goals>
                            <goal>scr</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <version>5.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        result = migrate_pom(pom_path, dry_run=False, stats=stats)

        self.assertTrue(result)
        self.assertGreater(stats.scr_plugins_removed, 0)
        self.assertGreater(stats.scr_deps_removed, 0)
        self.assertGreater(stats.osgi_deps_added, 0)

        # Verify final state
        content = pom_path.read_text()
        self.assertNotIn('maven-scr-plugin', content)
        self.assertNotIn('felix.scr.annotations', content)
        self.assertIn('org.osgi.service.component.annotations', content)
        self.assertIn('<extensions>true</extensions>', content)

    def test_osgi_dependencies_at_beginning(self):
        """Test that OSGi dependencies are added at the beginning of the dependency list."""
        pom_content = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
        </dependency>
        <dependency>
            <groupId>org.apache.felix</groupId>
            <artifactId>org.apache.felix.scr.annotations</artifactId>
            <version>1.12.0</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.32</version>
        </dependency>
    </dependencies>
</project>
'''
        pom_path = self.create_test_pom(pom_content)
        stats = PomStats()
        migrate_pom(pom_path, dry_run=False, stats=stats, add_metatype=True)

        # Read the result
        content = pom_path.read_text()
        tree = ET.parse(pom_path)
        root = tree.getroot()
        ns = '{http://maven.apache.org/POM/4.0.0}'

        dependencies = root.find(f'{ns}dependencies')
        self.assertIsNotNone(dependencies)

        dep_list = dependencies.findall(f'{ns}dependency')
        self.assertGreater(len(dep_list), 0)

        # First dependency should be component annotations
        first_artifact = dep_list[0].find(f'{ns}artifactId')
        self.assertEqual(first_artifact.text, 'org.osgi.service.component.annotations')

        # Second dependency should be component (provides property type annotations)
        second_artifact = dep_list[1].find(f'{ns}artifactId')
        self.assertEqual(second_artifact.text, 'org.osgi.service.component')

        # Third dependency should be metatype annotations
        third_artifact = dep_list[2].find(f'{ns}artifactId')
        self.assertEqual(third_artifact.text, 'org.osgi.service.metatype.annotations')

        # Fourth should be junit (original first dependency)
        fourth_artifact = dep_list[3].find(f'{ns}artifactId')
        self.assertEqual(fourth_artifact.text, 'junit')


class TestMetatypeDetection(unittest.TestCase):
    """Test metatype usage detection."""

    def test_detect_metatype_true(self):
        """Test detecting metatype = true."""
        with TemporaryDirectory() as tmpdir:
            tmpdir_path = Path(tmpdir)
            java_file = tmpdir_path / "Test.java"
            java_file.write_text('''
@Component(metatype = true)
public class Test {
}
''')
            self.assertTrue(detect_metatype_usage(tmpdir_path))

    def test_detect_object_class_definition(self):
        """Test detecting @ObjectClassDefinition."""
        with TemporaryDirectory() as tmpdir:
            tmpdir_path = Path(tmpdir)
            java_file = tmpdir_path / "Test.java"
            java_file.write_text('''
@ObjectClassDefinition(name = "Config")
public @interface Config {
}
''')
            self.assertTrue(detect_metatype_usage(tmpdir_path))

    def test_detect_property_with_label(self):
        """Test detecting @Property with label."""
        with TemporaryDirectory() as tmpdir:
            tmpdir_path = Path(tmpdir)
            java_file = tmpdir_path / "Test.java"
            java_file.write_text('''
@Property(name = "timeout", label = "Timeout", intValue = 30)
public class Test {
}
''')
            self.assertTrue(detect_metatype_usage(tmpdir_path))

    def test_no_metatype(self):
        """Test when no metatype is used."""
        with TemporaryDirectory() as tmpdir:
            tmpdir_path = Path(tmpdir)
            java_file = tmpdir_path / "Test.java"
            java_file.write_text('''
@Component
public class Test {
}
''')
            self.assertFalse(detect_metatype_usage(tmpdir_path))


class TestPomStats(unittest.TestCase):
    """Test POM statistics tracking."""

    def test_stats_initialization(self):
        """Test stats initialization."""
        stats = PomStats()
        self.assertEqual(stats.files_processed, 0)
        self.assertEqual(stats.files_changed, 0)
        self.assertEqual(len(stats.errors), 0)
        self.assertEqual(len(stats.warnings), 0)

    def test_stats_tracking(self):
        """Test stats tracking during migration."""
        stats = PomStats()
        stats.files_processed = 5
        stats.files_changed = 3
        stats.scr_plugins_removed = 2
        stats.osgi_deps_added = 3

        self.assertEqual(stats.files_processed, 5)
        self.assertEqual(stats.files_changed, 3)

    def test_error_tracking(self):
        """Test error tracking."""
        stats = PomStats()
        stats.add_error(Path("test.xml"), "Test error")

        self.assertEqual(len(stats.errors), 1)
        self.assertIn("Test error", stats.errors[0])

    def test_warning_tracking(self):
        """Test warning tracking."""
        stats = PomStats()
        stats.add_warning(Path("test.xml"), "Test warning")

        self.assertEqual(len(stats.warnings), 1)
        self.assertIn("Test warning", stats.warnings[0])


def run_tests():
    """Run all tests."""
    loader = unittest.TestLoader()
    suite = loader.loadTestsFromModule(sys.modules[__name__])
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == '__main__':
    sys.exit(run_tests())
