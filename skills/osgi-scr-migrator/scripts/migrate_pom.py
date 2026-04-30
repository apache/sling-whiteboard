#!/usr/bin/env python3
"""
POM Migration Script for OSGi SCR to R6/R7 (Enhanced)

Migrates Maven POM files to:
1. Remove maven-scr-plugin
2. Remove felix.scr.annotations dependency
3. Add osgi.service.component.annotations dependency
4. Add osgi.service.component dependency (provides property type annotations)
5. Add osgi.service.metatype.annotations dependency (when needed)
6. Update maven-bundle-plugin configuration
7. Smart version detection from existing dependencies

Features:
- Smart version detection from project
- Automatic property type annotations support (@ServiceRanking, @ServiceDescription, @ServiceVendor)
- Automatic metatype dependency addition
- Validation and reporting
- Preserves XML formatting
"""

import re
import sys
from pathlib import Path
from typing import Optional, Dict, List
from dataclasses import dataclass, field

# Try to use lxml for comment preservation, fallback to ElementTree
try:
    from lxml import etree as ET
    USING_LXML = True
except ImportError:
    import xml.etree.ElementTree as ET
    USING_LXML = False
    print("Warning: lxml not available. POM comments will be removed. Install lxml to preserve comments: pip install lxml", file=sys.stderr)


@dataclass
class PomStats:
    """Track POM migration statistics."""
    files_processed: int = 0
    files_changed: int = 0
    scr_plugins_removed: int = 0
    scr_deps_removed: int = 0
    osgi_deps_added: int = 0
    metatype_deps_added: int = 0
    sling_deps_added: int = 0
    bundle_plugins_updated: int = 0
    errors: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)

    def add_error(self, file_path: Path, message: str):
        """Add an error message."""
        self.errors.append(f"{file_path}: {message}")

    def add_warning(self, file_path: Path, message: str):
        """Add a warning message."""
        self.warnings.append(f"{file_path}: {message}")

    def print_summary(self):
        """Print migration summary."""
        print("\n" + "=" * 70)
        print("POM MIGRATION SUMMARY")
        print("=" * 70)
        print(f"Files processed:              {self.files_processed}")
        print(f"Files changed:                {self.files_changed}")
        print(f"SCR plugins removed:          {self.scr_plugins_removed}")
        print(f"SCR dependencies removed:     {self.scr_deps_removed}")
        print(f"OSGi dependencies added:      {self.osgi_deps_added}")
        print(f"Metatype dependencies added:  {self.metatype_deps_added}")
        print(f"Sling dependencies added:     {self.sling_deps_added}")
        print(f"Bundle plugins updated:       {self.bundle_plugins_updated}")

        if self.warnings:
            print(f"\nWarnings: {len(self.warnings)}")
            for warning in self.warnings[:10]:
                print(f"  ⚠ {warning}")
            if len(self.warnings) > 10:
                print(f"  ... and {len(self.warnings) - 10} more")

        if self.errors:
            print(f"\nErrors: {len(self.errors)}")
            for error in self.errors[:10]:
                print(f"  ✗ {error}")
            if len(self.errors) > 10:
                print(f"  ... and {len(self.errors) - 10} more")

        print("=" * 70)


class PomMigrator:
    # Latest stable versions (defaults)
    # IMPORTANT: Always use these exact versions for OSGi R6/R7 compatibility
    # - org.osgi.service.component.annotations: 1.5.1 (latest stable, required - provides @Component, @Reference, etc.)
    # - org.osgi.service.component: 1.5.1 (latest stable, required - provides property type annotations like @ServiceRanking, @ServiceDescription, @ServiceVendor)
    # - org.osgi.service.metatype.annotations: 1.4.1 (latest stable, required when metatype is used)
    # - org.apache.sling.servlets.annotations: 1.2.6 (latest stable, for Sling projects)
    DEFAULT_OSGI_VERSION = "1.5.1"
    DEFAULT_METATYPE_VERSION = "1.4.1"
    DEFAULT_SLING_VERSION = "1.2.6"

    def __init__(self, pom_path: Path, stats: PomStats, detect_versions: bool = True):
        self.pom_path = pom_path
        self.stats = stats
        self.detect_versions = detect_versions
        self.tree = None
        self.root = None
        self.namespace = ''
        self.changes_made = False
        self.osgi_version = self.DEFAULT_OSGI_VERSION
        self.metatype_version = self.DEFAULT_METATYPE_VERSION
        self.sling_version = self.DEFAULT_SLING_VERSION
        self.needs_metatype = False
        self.needs_sling = False

    def load(self) -> bool:
        """Load and parse the POM file."""
        try:
            # Parse with comment preservation when using lxml
            if USING_LXML:
                parser = ET.XMLParser(remove_blank_text=False, remove_comments=False)
                self.tree = ET.parse(str(self.pom_path), parser)
            else:
                self.tree = ET.parse(self.pom_path)

            self.root = self.tree.getroot()

            # Extract namespace if present
            if self.root.tag.startswith('{'):
                self.namespace = self.root.tag.split('}')[0] + '}'

            return True
        except Exception as e:
            self.stats.add_error(self.pom_path, f"Failed to load: {e}")
            return False

    def migrate(self, add_metatype: bool = False, add_sling: bool = False) -> bool:
        """Perform all migration steps."""
        self.needs_metatype = add_metatype
        self.needs_sling = add_sling

        if self.detect_versions:
            self._detect_osgi_versions()

        self._remove_scr_plugin()
        self._update_dependencies()
        self._update_bundle_plugin()

        return self.changes_made

    def _detect_osgi_versions(self):
        """Detect OSGi versions from existing dependencies."""
        dependencies = self._find_element('dependencies')
        if dependencies is None:
            # Try dependencyManagement
            dep_mgmt = self._find_element('dependencyManagement')
            if dep_mgmt is not None:
                dependencies = self._find_element('dependencies', dep_mgmt)

        if dependencies is not None:
            # Look for existing OSGi dependencies to infer version
            for dependency in dependencies.findall(f'{self.namespace}dependency'):
                group_id = dependency.find(f'{self.namespace}groupId')
                artifact_id = dependency.find(f'{self.namespace}artifactId')
                version = dependency.find(f'{self.namespace}version')

                if group_id is not None and group_id.text == 'org.osgi':
                    if version is not None and version.text:
                        version_text = version.text
                        # Remove properties placeholders
                        if not version_text.startswith('${'):
                            # Extract major.minor for consistency
                            if artifact_id.text in ['org.osgi.core', 'org.osgi.compendium']:
                                # Use compatible version
                                if version_text.startswith('6.') or version_text.startswith('7.'):
                                    self.osgi_version = "1.5.1"
                                    self.metatype_version = "1.4.1"
                                elif version_text.startswith('8.'):
                                    self.osgi_version = "1.5.1"
                                    self.metatype_version = "1.4.1"

    def _remove_scr_plugin(self):
        """Remove maven-scr-plugin from build plugins."""
        build = self._find_element('build')
        if build is None:
            return

        # Check pluginManagement first
        plugin_mgmt = self._find_element('pluginManagement', build)
        if plugin_mgmt is not None:
            plugins = self._find_element('plugins', plugin_mgmt)
            if plugins is not None:
                if self._remove_plugin_from_list(plugins, 'maven-scr-plugin'):
                    self.stats.scr_plugins_removed += 1

        # Check direct plugins
        plugins = self._find_element('plugins', build)
        if plugins is not None:
            if self._remove_plugin_from_list(plugins, 'maven-scr-plugin'):
                self.stats.scr_plugins_removed += 1

    def _remove_plugin_from_list(self, plugins_element, plugin_artifact_id: str) -> bool:
        """Remove a plugin from a plugins list."""
        removed = False
        for plugin in list(plugins_element.findall(f'{self.namespace}plugin')):
            artifact_id = plugin.find(f'{self.namespace}artifactId')
            if artifact_id is not None and artifact_id.text == plugin_artifact_id:
                plugins_element.remove(plugin)
                self.changes_made = True
                removed = True
                print(f"  Removed {plugin_artifact_id}")
        return removed

    def _update_dependencies(self):
        """Update dependencies: remove SCR annotations, add OSGi annotations."""
        dependencies = self._find_element('dependencies')
        if dependencies is None:
            # Create dependencies section if needed
            dependencies = ET.SubElement(self.root, f'{self.namespace}dependencies')

        # Remove felix.scr.annotations
        for dependency in list(dependencies.findall(f'{self.namespace}dependency')):
            artifact_id = dependency.find(f'{self.namespace}artifactId')
            if artifact_id is not None and artifact_id.text == 'org.apache.felix.scr.annotations':
                dependencies.remove(dependency)
                self.changes_made = True
                self.stats.scr_deps_removed += 1
                print("  Removed org.apache.felix.scr.annotations dependency")

        # Check if dependencies already exist
        has_osgi_annotations = self._has_dependency(dependencies, 'org.osgi.service.component.annotations')
        has_osgi_component = self._has_dependency(dependencies, 'org.osgi.service.component')
        has_metatype_annotations = self._has_dependency(dependencies, 'org.osgi.service.metatype.annotations')
        has_sling_annotations = self._has_dependency(dependencies, 'org.apache.sling.servlets.annotations')

        # Add osgi.service.component.annotations if not present
        if not has_osgi_annotations:
            self._add_osgi_annotations_dependency(dependencies)
            self.stats.osgi_deps_added += 1

        # Add osgi.service.component if not present (provides property type annotations)
        if not has_osgi_component:
            self._add_osgi_component_dependency(dependencies)
            self.stats.osgi_deps_added += 1

        # Add metatype annotations if needed
        if self.needs_metatype and not has_metatype_annotations:
            self._add_metatype_annotations_dependency(dependencies)
            self.stats.metatype_deps_added += 1

        # Add Sling annotations if needed
        if self.needs_sling and not has_sling_annotations:
            self._add_sling_annotations_dependency(dependencies)
            self.stats.sling_deps_added += 1

    def _has_dependency(self, dependencies_element, artifact_id: str) -> bool:
        """Check if a dependency already exists."""
        for dependency in dependencies_element.findall(f'{self.namespace}dependency'):
            dep_artifact_id = dependency.find(f'{self.namespace}artifactId')
            if dep_artifact_id is not None and dep_artifact_id.text == artifact_id:
                return True
        return False

    def _add_osgi_annotations_dependency(self, dependencies_element):
        """Add org.osgi.service.component.annotations dependency at the beginning."""
        dep = self._create_dependency(
            'org.osgi',
            'org.osgi.service.component.annotations',
            self.osgi_version,
            'provided'
        )
        # Insert at the beginning of the dependency list
        dependencies_element.insert(0, dep)
        self.changes_made = True
        print(f"  Added org.osgi.service.component.annotations {self.osgi_version}")

    def _add_osgi_component_dependency(self, dependencies_element):
        """Add org.osgi.service.component dependency after component annotations."""
        dep = self._create_dependency(
            'org.osgi',
            'org.osgi.service.component',
            self.osgi_version,
            'provided'
        )
        # Insert after component annotations if present, otherwise at the beginning
        insert_pos = 0
        if len(dependencies_element) > 0:
            first_dep = dependencies_element[0]
            first_artifact = first_dep.find(f'{self.namespace}artifactId')
            if first_artifact is not None and first_artifact.text == 'org.osgi.service.component.annotations':
                insert_pos = 1

        dependencies_element.insert(insert_pos, dep)
        self.changes_made = True
        print(f"  Added org.osgi.service.component {self.osgi_version}")

    def _add_metatype_annotations_dependency(self, dependencies_element):
        """Add org.osgi.service.metatype.annotations dependency after component annotations."""
        dep = self._create_dependency(
            'org.osgi',
            'org.osgi.service.metatype.annotations',
            self.metatype_version,
            'provided'
        )
        # Insert after both component.annotations and component dependencies if present
        insert_pos = 0
        osgi_count = 0
        for i, existing_dep in enumerate(dependencies_element):
            artifact = existing_dep.find(f'{self.namespace}artifactId')
            if artifact is not None and artifact.text in ['org.osgi.service.component.annotations',
                                                           'org.osgi.service.component']:
                osgi_count = i + 1

        insert_pos = osgi_count
        dependencies_element.insert(insert_pos, dep)
        self.changes_made = True
        print(f"  Added org.osgi.service.metatype.annotations {self.metatype_version}")

    def _add_sling_annotations_dependency(self, dependencies_element):
        """Add org.apache.sling.servlets.annotations dependency after OSGi annotations."""
        dep = self._create_dependency(
            'org.apache.sling',
            'org.apache.sling.servlets.annotations',
            self.sling_version,
            'provided'
        )
        # Insert after OSGi annotations (component.annotations, component, and optionally metatype)
        insert_pos = 0
        osgi_count = 0
        for i, existing_dep in enumerate(dependencies_element):
            artifact = existing_dep.find(f'{self.namespace}artifactId')
            if artifact is not None and artifact.text in ['org.osgi.service.component.annotations',
                                                           'org.osgi.service.component',
                                                           'org.osgi.service.metatype.annotations']:
                osgi_count = i + 1

        insert_pos = osgi_count
        dependencies_element.insert(insert_pos, dep)
        self.changes_made = True
        print(f"  Added org.apache.sling.servlets.annotations {self.sling_version}")

    def _create_dependency(self, group_id: str, artifact_id: str, version: str, scope: str = None) -> ET.Element:
        """Create a dependency element with proper indentation."""
        dep = ET.Element(f'{self.namespace}dependency')

        # Set proper indentation for the dependency element
        # Text before first child (newline + 3 levels of indent = 12 spaces)
        dep.text = '\n            '

        g = ET.SubElement(dep, f'{self.namespace}groupId')
        g.text = group_id
        g.tail = '\n            '

        a = ET.SubElement(dep, f'{self.namespace}artifactId')
        a.text = artifact_id
        a.tail = '\n            '

        v = ET.SubElement(dep, f'{self.namespace}version')
        v.text = version

        if scope:
            v.tail = '\n            '
            s = ET.SubElement(dep, f'{self.namespace}scope')
            s.text = scope
            s.tail = '\n        '
        else:
            v.tail = '\n        '

        # Tail after closing </dependency> tag (newline + 2 levels of indent = 8 spaces)
        dep.tail = '\n        '

        return dep

    def _update_bundle_plugin(self):
        """Update OSGi bundle plugin (maven-bundle-plugin or bnd-maven-plugin) configuration if needed."""
        build = self._find_element('build')
        if build is None:
            return

        plugins = self._find_element('plugins', build)
        if plugins is None:
            return

        # Find either maven-bundle-plugin or bnd-maven-plugin
        bundle_plugins = ['maven-bundle-plugin', 'bnd-maven-plugin']

        for plugin in plugins.findall(f'{self.namespace}plugin'):
            artifact_id = plugin.find(f'{self.namespace}artifactId')
            if artifact_id is not None and artifact_id.text in bundle_plugins:
                plugin_name = artifact_id.text
                updated = False

                # Ensure extensions is true (both plugins support this)
                extensions = plugin.find(f'{self.namespace}extensions')
                if extensions is None:
                    extensions = ET.SubElement(plugin, f'{self.namespace}extensions')
                    extensions.text = 'true'
                    updated = True
                    print(f"  Updated {plugin_name} to use extensions=true")
                elif extensions.text != 'true':
                    extensions.text = 'true'
                    updated = True
                    print(f"  Updated {plugin_name} extensions to true")

                # Check for SCR instructions that should be removed
                configuration = plugin.find(f'{self.namespace}configuration')
                if configuration is not None:
                    instructions = configuration.find(f'{self.namespace}instructions')
                    if instructions is not None:
                        # Remove SCR-specific instructions
                        scr_elements = [
                            'Service-Component',
                            '_dsannotations',
                            '_metatype'
                        ]
                        for elem_name in scr_elements:
                            elem = instructions.find(f'{self.namespace}{elem_name}')
                            if elem is not None:
                                instructions.remove(elem)
                                updated = True
                                print(f"  Removed {elem_name} instruction from {plugin_name}")

                if updated:
                    self.changes_made = True
                    self.stats.bundle_plugins_updated += 1

    def _find_element(self, tag: str, parent=None) -> Optional[ET.Element]:
        """Find an element with the given tag, accounting for namespace."""
        if parent is None:
            parent = self.root
        return parent.find(f'{self.namespace}{tag}')

    def save(self, dry_run: bool = False):
        """Save the modified POM file."""
        if not self.changes_made:
            return

        if dry_run:
            print(f"[DRY RUN] Would save changes to {self.pom_path}")
            return

        try:
            # Write XML preserving comments and formatting
            if USING_LXML:
                # lxml preserves comments and formatting
                # lxml handles namespaces automatically, no need to register
                self.tree.write(
                    str(self.pom_path),
                    encoding='utf-8',
                    xml_declaration=True,
                    pretty_print=True
                )
            else:
                # Register namespace to avoid ns0: prefix (ElementTree only)
                if self.namespace:
                    ET.register_namespace('', self.namespace.strip('{}'))

                # ElementTree doesn't preserve comments (user will see warning)
                self.tree.write(
                    self.pom_path,
                    encoding='utf-8',
                    xml_declaration=True
                )
            print(f"Saved changes to {self.pom_path}")
        except Exception as e:
            self.stats.add_error(self.pom_path, f"Failed to save: {e}")

    def _indent(self, elem, level=0):
        """Add pretty-print indentation to XML."""
        i = "\n" + level * "  "
        if len(elem):
            if not elem.text or not elem.text.strip():
                elem.text = i + "  "
            if not elem.tail or not elem.tail.strip():
                elem.tail = i
            for child in elem:
                self._indent(child, level + 1)
            if not child.tail or not child.tail.strip():
                child.tail = i
        else:
            if level and (not elem.tail or not elem.tail.strip()):
                elem.tail = i


def detect_metatype_usage(directory: Path) -> bool:
    """Detect if Java files use metatype annotations."""
    java_files = list(directory.rglob('*.java'))
    for java_file in java_files:
        try:
            content = java_file.read_text(encoding='utf-8')
            # Check for metatype indicators
            if re.search(r'metatype\s*=\s*true', content):
                return True
            if re.search(r'@ObjectClassDefinition', content):
                return True
            if re.search(r'label\s*=\s*"', content) and '@Property' in content:
                return True
        except:
            pass
    return False


def detect_sling_usage(directory: Path) -> bool:
    """Detect if Java files use Sling servlet/filter annotations."""
    java_files = list(directory.rglob('*.java'))
    for java_file in java_files:
        try:
            content = java_file.read_text(encoding='utf-8')
            # Check for Sling-specific annotations
            if '@SlingServlet' in content:
                return True
            if '@SlingFilter' in content:
                return True
            # Also check for already-migrated Sling Servlets Annotations
            if '@SlingServletPaths' in content:
                return True
            if '@SlingServletResourceTypes' in content:
                return True
            if '@SlingServletFilter' in content:
                return True
        except:
            pass
    return False


def migrate_pom(pom_path: Path, dry_run: bool = False, stats: PomStats = None,
                detect_versions: bool = True, add_metatype: bool = False, add_sling: bool = False) -> bool:
    """Migrate a single POM file."""
    if stats is None:
        stats = PomStats()

    stats.files_processed += 1
    print(f"\nProcessing: {pom_path}")

    migrator = PomMigrator(pom_path, stats, detect_versions)
    if not migrator.load():
        return False

    if migrator.migrate(add_metatype, add_sling):
        migrator.save(dry_run)
        stats.files_changed += 1
        return True
    else:
        print("  No changes needed")
        return False


def find_pom_files(directory: Path) -> List[Path]:
    """Find all pom.xml files in the directory."""
    return list(directory.rglob('pom.xml'))


def validate_pom(pom_path: Path, stats: PomStats) -> bool:
    """Validate migrated POM for common issues."""
    try:
        content = pom_path.read_text(encoding='utf-8')

        # Check for remaining SCR references
        if 'felix.scr.annotations' in content:
            stats.add_warning(pom_path, "Still contains felix.scr.annotations")

        if 'maven-scr-plugin' in content:
            stats.add_warning(pom_path, "Still contains maven-scr-plugin")

        # Check for OSGi annotations
        if 'org.osgi.service.component.annotations' not in content:
            stats.add_warning(pom_path, "Missing org.osgi.service.component.annotations dependency")

        # Check for OSGi component (provides property type annotations)
        if 'org.osgi.service.component</artifactId>' not in content:
            stats.add_warning(pom_path, "Missing org.osgi.service.component dependency (required for @ServiceRanking, @ServiceDescription, @ServiceVendor)")

        return True
    except Exception as e:
        stats.add_error(pom_path, f"Validation error: {e}")
        return False


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description='Migrate Maven POM from Felix SCR to OSGi R6/R7 (Enhanced)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s /path/to/project                    # Migrate all POM files
  %(prog)s /path/to/project --dry-run          # Preview changes
  %(prog)s /path/to/pom.xml                    # Migrate single POM
  %(prog)s /path/to/project --add-metatype     # Add metatype dependency
  %(prog)s /path/to/project --validate         # Validate after migration
        """
    )
    parser.add_argument(
        'path',
        type=Path,
        help='Path to pom.xml file or directory containing pom.xml files'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be changed without modifying files'
    )
    parser.add_argument(
        '--validate',
        action='store_true',
        help='Validate migrated POMs for common issues'
    )
    parser.add_argument(
        '--add-metatype',
        action='store_true',
        help='Add metatype annotations dependency'
    )
    parser.add_argument(
        '--auto-detect-metatype',
        action='store_true',
        help='Automatically detect if metatype is needed by scanning Java files'
    )
    parser.add_argument(
        '--add-sling',
        action='store_true',
        help='Add Sling Servlets Annotations dependency'
    )
    parser.add_argument(
        '--auto-detect-sling',
        action='store_true',
        dest='auto_detect_sling',
        default=True,
        help='Automatically detect if Sling is needed by scanning Java files (default: enabled)'
    )
    parser.add_argument(
        '--no-auto-detect-sling',
        action='store_false',
        dest='auto_detect_sling',
        help='Disable automatic Sling detection'
    )
    parser.add_argument(
        '--detect-versions',
        action='store_true',
        default=True,
        help='Detect appropriate OSGi versions from existing dependencies (default: True)'
    )

    args = parser.parse_args()

    if not args.path.exists():
        print(f"Error: Path does not exist: {args.path}", file=sys.stderr)
        sys.exit(1)

    # Find files to process
    if args.path.is_file() and args.path.name == 'pom.xml':
        files = [args.path]
        project_dir = args.path.parent
    elif args.path.is_dir():
        files = find_pom_files(args.path)
        project_dir = args.path
    else:
        print(f"Error: Path must be pom.xml file or directory", file=sys.stderr)
        sys.exit(1)

    # Auto-detect metatype need
    add_metatype = args.add_metatype
    if args.auto_detect_metatype:
        print("Scanning Java files for metatype usage...")
        if detect_metatype_usage(project_dir):
            add_metatype = True
            print("  Metatype usage detected, will add metatype dependency")
        else:
            print("  No metatype usage detected")

    # Auto-detect Sling need (enabled by default)
    add_sling = args.add_sling
    if args.auto_detect_sling:
        print("Scanning Java files for Sling usage...")
        if detect_sling_usage(project_dir):
            add_sling = True
            print("  Sling usage detected, will add Sling Servlets Annotations dependency")
        else:
            print("  No Sling usage detected")

    print(f"Found {len(files)} POM files to process")
    print("=" * 70)

    # Process each file
    stats = PomStats()
    for file_path in files:
        migrate_pom(file_path, args.dry_run, stats, args.detect_versions, add_metatype, add_sling)
        if args.validate and not args.dry_run:
            validate_pom(file_path, stats)

    # Print summary
    stats.print_summary()

    # Exit with error code if there were errors
    if stats.errors:
        sys.exit(1)


if __name__ == '__main__':
    main()
