#!/usr/bin/env python3
"""
OSGi SCR to R6/R7 Annotation Migration Script (Enhanced)

Migrates Java files from deprecated Felix SCR annotations to official OSGi Component annotations.
Handles @Component, @Service, @Reference, @Property, @Activate, @Deactivate, and Sling-specific annotations.

Features:
- Full @Property migration with type specifications
- Metatype detection and configuration interface generation
- Complete Sling annotation transformation
- Validation and reporting
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple, Set, Dict, Optional
from dataclasses import dataclass, field
from enum import Enum


class PropertyType(Enum):
    """Property value types."""
    STRING = "String"
    BOOLEAN = "Boolean"
    INTEGER = "Integer"
    LONG = "Long"
    FLOAT = "Float"
    DOUBLE = "Double"


@dataclass
class Property:
    """Represents a component property."""
    name: str
    value: Optional[str] = None
    values: List[str] = field(default_factory=list)
    type: PropertyType = PropertyType.STRING
    label: Optional[str] = None
    description: Optional[str] = None
    is_configurable: bool = False

    def is_property_type_annotation(self) -> bool:
        """Check if this property should use a Component Property Type annotation.

        Returns True only if:
        1. Property name matches a known property type annotation
        2. Property has the correct type for that annotation
        3. Property is not configurable (no label/description)
        4. Property is single-valued (not array)
        """
        # Only single-value, non-configurable properties can use property type annotations
        if self.is_configurable or self.values:
            return False

        # Must have a value to convert
        if self.value is None:
            return False

        # Map property names to their expected types
        PROPERTY_TYPE_REQUIREMENTS = {
            'service.ranking': PropertyType.INTEGER,
            'service.description': PropertyType.STRING,
            'service.vendor': PropertyType.STRING,
        }

        expected_type = PROPERTY_TYPE_REQUIREMENTS.get(self.name)
        if expected_type is None:
            return False

        # Must match expected type
        return self.type == expected_type

    def to_property_type_annotation(self) -> str:
        """Convert to Component Property Type annotation.

        Precondition: is_property_type_annotation() returned True.
        This method should always succeed if the precondition is met.
        """
        if self.name == 'service.ranking':
            return f'@ServiceRanking({self.value})'
        elif self.name == 'service.description':
            return f'@ServiceDescription("{self.value}")'
        elif self.name == 'service.vendor':
            return f'@ServiceVendor("{self.value}")'

        # Should never reach here if precondition is met
        raise ValueError(f"Unknown property type annotation: {self.name}")

    def to_component_property(self) -> List[str]:
        """Convert to @Component property format."""
        properties = []
        type_suffix = f":{self.type.value}" if self.type != PropertyType.STRING else ""

        if self.values:
            for val in self.values:
                properties.append(f'"{self.name}{type_suffix}={val}"')
        elif self.value is not None:
            properties.append(f'"{self.name}{type_suffix}={self.value}"')

        return properties

    def to_metatype_attribute(self, indent: str = '    ') -> str:
        """Convert to @AttributeDefinition format.

        Args:
            indent: The indentation string to use (default: 4 spaces)
        """
        parts = []
        if self.label:
            parts.append(f'name = "{self.label}"')
        if self.description:
            parts.append(f'description = "{self.description}"')

        method_name = self.name.replace('.', '_').replace('-', '_')
        java_type = self._get_java_type()
        default_value = self._get_default_value()

        attrs = ', '.join(parts) if parts else ''
        attr_annotation = f'@AttributeDefinition({attrs})' if attrs else '@AttributeDefinition'

        return f'{indent}{attr_annotation}\n{indent}{java_type} {method_name}(){default_value};'

    def _get_java_type(self) -> str:
        """Get Java type for metatype."""
        type_map = {
            PropertyType.STRING: "String",
            PropertyType.BOOLEAN: "boolean",
            PropertyType.INTEGER: "int",
            PropertyType.LONG: "long",
            PropertyType.FLOAT: "float",
            PropertyType.DOUBLE: "double",
        }
        if self.values and len(self.values) > 1:
            return f"{type_map[self.type]}[]"
        return type_map[self.type]

    def _get_default_value(self) -> str:
        """Get default value expression."""
        if self.values:
            if self.type == PropertyType.STRING:
                vals = ', '.join(f'"{v}"' for v in self.values)
                return f' default {{{vals}}}'
            else:
                vals = ', '.join(self.values)
                return f' default {{{vals}}}'
        elif self.value is not None:
            if self.type == PropertyType.STRING:
                return f' default "{self.value}"'
            else:
                return f' default {self.value}'
        return ''


@dataclass
class MigrationStats:
    """Track migration statistics."""
    files_processed: int = 0
    files_changed: int = 0
    components_migrated: int = 0
    services_merged: int = 0
    references_updated: int = 0
    properties_migrated: int = 0
    property_type_annotations_added: int = 0
    sling_servlets_migrated: int = 0
    sling_filters_migrated: int = 0
    metatype_configs_generated: int = 0
    imports_updated: int = 0
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
        print("MIGRATION SUMMARY")
        print("=" * 70)
        print(f"Files processed:              {self.files_processed}")
        print(f"Files changed:                {self.files_changed}")
        print(f"@Component annotations:       {self.components_migrated}")
        print(f"@Service merged:              {self.services_merged}")
        print(f"@Reference updated:           {self.references_updated}")
        print(f"@Property migrated:           {self.properties_migrated}")
        print(f"Property type annotations:    {self.property_type_annotations_added}")
        print(f"@SlingServlet migrated:       {self.sling_servlets_migrated}")
        print(f"@SlingFilter migrated:        {self.sling_filters_migrated}")
        print(f"Metatype configs generated:   {self.metatype_configs_generated}")
        print(f"Import statements updated:    {self.imports_updated}")

        if self.warnings:
            print(f"\nWarnings: {len(self.warnings)}")
            for warning in self.warnings[:10]:  # Show first 10
                print(f"  ⚠ {warning}")
            if len(self.warnings) > 10:
                print(f"  ... and {len(self.warnings) - 10} more")

        if self.errors:
            print(f"\nErrors: {len(self.errors)}")
            for error in self.errors[:10]:  # Show first 10
                print(f"  ✗ {error}")
            if len(self.errors) > 10:
                print(f"  ... and {len(self.errors) - 10} more")

        print("=" * 70)


class AnnotationMigrator:
    def __init__(self, content: str, file_path: Path, stats: MigrationStats):
        self.content = content
        self.file_path = file_path
        self.lines = content.split('\n')
        self.stats = stats
        self.imports_to_remove: Set[str] = set()
        self.imports_to_add: Set[str] = set()
        self.changes_made = False
        self.component_properties: List[Property] = []
        self.has_metatype = False
        self.component_name = ""
        self.component_label: Optional[str] = None
        self.component_description: Optional[str] = None

    def migrate(self) -> Tuple[str, bool]:
        """Migrate all SCR annotations to OSGi R6/R7 annotations."""
        try:
            self._extract_class_name()
            self._detect_metatype()
            self._collect_properties()
            self._migrate_imports()
            self._migrate_sling_servlet()
            self._migrate_sling_filter()
            self._migrate_component_annotation()  # MUST come before _migrate_service_annotation
            self._migrate_service_annotation()     # so we can find @Service during component migration
            self._migrate_reference_annotations()
            self._migrate_lifecycle_annotations()

            # Generate metatype config before updating imports so metatype imports are included
            if self.has_metatype and any(p.is_configurable for p in self.component_properties):
                self._generate_metatype_config()

            self._update_imports()

            return '\n'.join(self.lines), self.changes_made
        except Exception as e:
            self.stats.add_error(self.file_path, str(e))
            return self.content, False

    def _extract_class_name(self):
        """Extract the class name for generating config interface."""
        for line in self.lines:
            match = re.search(r'public\s+(?:abstract\s+)?class\s+(\w+)', line)
            if match:
                self.component_name = match.group(1)
                break

    def _detect_metatype(self):
        """Detect if component uses metatype and extract label/description."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]
            if '@Component' in line and not line.strip().startswith('//'):
                # Collect full annotation (may span multiple lines)
                annotation = line
                paren_count = line.count('(') - line.count(')')
                j = i + 1
                while paren_count > 0 and j < len(self.lines):
                    annotation += ' ' + self.lines[j].strip()
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    j += 1

                # Check for metatype
                if re.search(r'metatype\s*=\s*true', annotation):
                    self.has_metatype = True
                    self.stats.metatype_configs_generated += 1

                # Extract label and description from @Component
                label_match = re.search(r'label\s*=\s*"([^"]*)"', annotation)
                if label_match:
                    self.component_label = label_match.group(1)

                desc_match = re.search(r'description\s*=\s*"([^"]*)"', annotation)
                if desc_match:
                    self.component_description = desc_match.group(1)

                break
            i += 1

    def _collect_properties(self):
        """Collect all @Property annotations."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if '@Property' in line and not line.strip().startswith('//'):
                prop = self._parse_property_annotation(i)
                if prop:
                    self.component_properties.append(prop)

                    # Check for property type annotation candidates with wrong types
                    if prop.name in ['service.ranking', 'service.description', 'service.vendor']:
                        expected_types = {
                            'service.ranking': PropertyType.INTEGER,
                            'service.description': PropertyType.STRING,
                            'service.vendor': PropertyType.STRING,
                        }
                        expected = expected_types.get(prop.name)
                        if expected and prop.type != expected:
                            self.stats.add_warning(
                                self.file_path,
                                f"Property '{prop.name}' has type {prop.type.value}, expected {expected.value}. "
                                f"Will use property string instead of @{prop.name.split('.')[-1].title()} annotation."
                            )

                    # Mark for manual review if it has label/description (might need metatype)
                    if prop.label or prop.description:
                        prop.is_configurable = True
                    self.stats.properties_migrated += 1
            i += 1

    def _parse_property_annotation(self, start_idx: int) -> Optional[Property]:
        """Parse a @Property annotation."""
        # Collect the full annotation (may span multiple lines)
        annotation = self.lines[start_idx]
        paren_count = annotation.count('(') - annotation.count(')')
        idx = start_idx + 1

        while paren_count > 0 and idx < len(self.lines):
            annotation += ' ' + self.lines[idx].strip()
            paren_count += self.lines[idx].count('(') - self.lines[idx].count(')')
            idx += 1

        # Parse attributes
        name_match = re.search(r'name\s*=\s*"([^"]*)"', annotation)
        value_match = re.search(r'(?<!bool)(?<!int)(?<!long)(?<!float)(?<!double)value\s*=\s*"([^"]*)"', annotation)
        values_match = re.search(r'value\s*=\s*\{([^}]+)\}', annotation)
        bool_value_match = re.search(r'boolValue\s*=\s*(true|false)', annotation)
        int_value_match = re.search(r'intValue\s*=\s*(-?\d+)', annotation)
        long_value_match = re.search(r'longValue\s*=\s*(-?\d+)L?', annotation)
        float_value_match = re.search(r'floatValue\s*=\s*(-?\d+\.?\d*)f?', annotation)
        double_value_match = re.search(r'doubleValue\s*=\s*(-?\d+\.?\d*)', annotation)
        label_match = re.search(r'label\s*=\s*"([^"]*)"', annotation)
        desc_match = re.search(r'description\s*=\s*"([^"]*)"', annotation)

        # If no name attribute, look for the property name in the field declaration
        # Pattern: @Property(...) \n private static final String FIELD_NAME = "property.name";
        property_name = None
        if name_match:
            property_name = name_match.group(1)
        else:
            # Look for the field declaration on the next line(s) after the annotation
            for i in range(idx, min(idx + 3, len(self.lines))):
                field_line = self.lines[i]
                # Match: private static final String FIELD_NAME = "property.name";
                field_match = re.search(r'private\s+static\s+final\s+String\s+\w+\s*=\s*"([^"]+)"', field_line)
                if field_match:
                    property_name = field_match.group(1)
                    break

        if not property_name:
            return None

        prop = Property(name=property_name)

        # Determine type and value
        if bool_value_match:
            prop.type = PropertyType.BOOLEAN
            prop.value = bool_value_match.group(1)
        elif int_value_match:
            prop.type = PropertyType.INTEGER
            prop.value = int_value_match.group(1)
        elif long_value_match:
            prop.type = PropertyType.LONG
            prop.value = long_value_match.group(1)
        elif float_value_match:
            prop.type = PropertyType.FLOAT
            prop.value = float_value_match.group(1)
        elif double_value_match:
            prop.type = PropertyType.DOUBLE
            prop.value = double_value_match.group(1)
        elif values_match:
            # Array of values
            values_str = values_match.group(1)
            prop.values = [v.strip().strip('"') for v in values_str.split(',')]
        elif value_match:
            prop.value = value_match.group(1)

        if label_match:
            prop.label = label_match.group(1)
        if desc_match:
            prop.description = desc_match.group(1)

        return prop

    def _migrate_imports(self):
        """Identify SCR imports that need to be replaced."""
        scr_imports = {
            'org.apache.felix.scr.annotations.Activate': 'org.osgi.service.component.annotations.Activate',
            'org.apache.felix.scr.annotations.Component': 'org.osgi.service.component.annotations.Component',
            'org.apache.felix.scr.annotations.Deactivate': 'org.osgi.service.component.annotations.Deactivate',
            'org.apache.felix.scr.annotations.Modified': 'org.osgi.service.component.annotations.Modified',
            'org.apache.felix.scr.annotations.Property': None,
            'org.apache.felix.scr.annotations.Reference': 'org.osgi.service.component.annotations.Reference',
            'org.apache.felix.scr.annotations.ReferenceCardinality': 'org.osgi.service.component.annotations.ReferenceCardinality',
            'org.apache.felix.scr.annotations.ReferencePolicy': 'org.osgi.service.component.annotations.ReferencePolicy',
            'org.apache.felix.scr.annotations.ReferencePolicyOption': 'org.osgi.service.component.annotations.ReferencePolicyOption',
            'org.apache.felix.scr.annotations.Service': None,
            'org.apache.felix.scr.annotations.sling.SlingServlet': None,
            'org.apache.felix.scr.annotations.sling.SlingFilter': None,
        }

        for i, line in enumerate(self.lines):
            for scr_import, osgi_import in scr_imports.items():
                if f'import {scr_import}' in line:
                    self.imports_to_remove.add(scr_import)
                    if osgi_import:
                        self.imports_to_add.add(osgi_import)
                    self.changes_made = True
                    self.stats.imports_updated += 1

    def _migrate_sling_servlet(self):
        """Migrate @SlingServlet to @Component with Sling Servlets Annotations."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if '@SlingServlet' in line and not line.strip().startswith('//'):
                # Collect full annotation
                annotation = line
                paren_count = line.count('(') - line.count(')')
                j = i + 1
                annotation_lines = [i]

                while paren_count > 0 and j < len(self.lines):
                    annotation += ' ' + self.lines[j].strip()
                    annotation_lines.append(j)
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    j += 1

                # Parse attributes
                indent = len(self.lines[annotation_lines[0]]) - len(self.lines[annotation_lines[0]].lstrip())
                indent_str = ' ' * indent

                # Determine if path-based or resource-type-based
                paths_match = re.search(r'paths\s*=\s*\{([^}]+)\}', annotation)
                path_match = re.search(r'paths\s*=\s*"([^"]+)"', annotation) if not paths_match else None
                rt_match = re.search(r'resourceTypes\s*=\s*\{([^}]+)\}', annotation)
                rt_single_match = re.search(r'resourceTypes\s*=\s*"([^"]+)"', annotation) if not rt_match else None
                # Also match constant references like ActivityManagerImpl.RESOURCE_TYPE
                rt_constant_match = re.search(r'resourceTypes\s*=\s*([A-Za-z_][A-Za-z0-9_.]*)', annotation) if not (rt_match or rt_single_match) else None

                new_annotations = []

                # Add @Component annotation
                new_annotations.append(f'{indent_str}@Component(service = Servlet.class)')

                # Path-based servlet
                if paths_match or path_match:
                    if paths_match:
                        paths = [p.strip().strip('"') for p in paths_match.group(1).split(',')]
                        paths_str = ', '.join(f'"{p}"' for p in paths)
                        new_annotations.append(f'{indent_str}@SlingServletPaths(value = {{{paths_str}}})')
                    elif path_match:
                        new_annotations.append(f'{indent_str}@SlingServletPaths(value = "{path_match.group(1)}")')

                # Resource-type-based servlet
                elif rt_match or rt_single_match or rt_constant_match:
                    attrs = []

                    if rt_match:
                        # Parse array elements, preserving whether they're string literals or constants
                        raw_rts = [rt.strip() for rt in rt_match.group(1).split(',')]
                        formatted_rts = []
                        for rt in raw_rts:
                            if rt.startswith('"') and rt.endswith('"'):
                                # String literal - keep as is
                                formatted_rts.append(rt)
                            else:
                                # Constant reference - keep without quotes
                                formatted_rts.append(rt)

                        if len(formatted_rts) == 1:
                            # Single element - check if it needs quotes
                            if formatted_rts[0].startswith('"'):
                                attrs.append(f'resourceTypes = {formatted_rts[0]}')
                            else:
                                attrs.append(f'resourceTypes = {formatted_rts[0]}')
                        else:
                            rts_str = ', '.join(formatted_rts)
                            attrs.append(f'resourceTypes = {{{rts_str}}}')
                    elif rt_single_match:
                        attrs.append(f'resourceTypes = "{rt_single_match.group(1)}"')
                    elif rt_constant_match:
                        # Constant reference - use as-is without quotes
                        attrs.append(f'resourceTypes = {rt_constant_match.group(1)}')

                    # Selectors
                    selectors_match = re.search(r'selectors\s*=\s*\{([^}]+)\}', annotation)
                    selector_match = re.search(r'selectors\s*=\s*"([^"]+)"', annotation) if not selectors_match else None

                    if selectors_match:
                        sels = [s.strip().strip('"') for s in selectors_match.group(1).split(',')]
                        if len(sels) == 1:
                            attrs.append(f'selectors = "{sels[0]}"')
                        else:
                            sels_str = ', '.join(f'"{s}"' for s in sels)
                            attrs.append(f'selectors = {{{sels_str}}}')
                    elif selector_match:
                        attrs.append(f'selectors = "{selector_match.group(1)}"')

                    # Extensions
                    ext_match = re.search(r'extensions\s*=\s*\{([^}]+)\}', annotation)
                    ext_single_match = re.search(r'extensions\s*=\s*"([^"]+)"', annotation) if not ext_match else None

                    if ext_match:
                        exts = [e.strip().strip('"') for e in ext_match.group(1).split(',')]
                        if len(exts) == 1:
                            attrs.append(f'extensions = "{exts[0]}"')
                        else:
                            exts_str = ', '.join(f'"{e}"' for e in exts)
                            attrs.append(f'extensions = {{{exts_str}}}')
                    elif ext_single_match:
                        attrs.append(f'extensions = "{ext_single_match.group(1)}"')

                    # Methods
                    methods_match = re.search(r'methods\s*=\s*\{([^}]+)\}', annotation)
                    method_match = re.search(r'methods\s*=\s*"([^"]+)"', annotation) if not methods_match else None

                    if methods_match:
                        methods = [m.strip().strip('"') for m in methods_match.group(1).split(',')]
                        if len(methods) == 1:
                            attrs.append(f'methods = "{methods[0]}"')
                        else:
                            methods_str = ', '.join(f'"{m}"' for m in methods)
                            attrs.append(f'methods = {{{methods_str}}}')
                    elif method_match:
                        attrs.append(f'methods = "{method_match.group(1)}"')

                    # Build @SlingServletResourceTypes annotation
                    if len(attrs) == 1:
                        new_annotations.append(f'{indent_str}@SlingServletResourceTypes({attrs[0]})')
                    else:
                        attrs_str = ',\n    '.join(attrs)
                        new_annotations.append(
                            f'{indent_str}@SlingServletResourceTypes(\n'
                            f'{indent_str}    {attrs_str}\n'
                            f'{indent_str})'
                        )

                # Replace the annotation
                for idx in reversed(annotation_lines):
                    del self.lines[idx]

                # Insert new annotations in reverse order
                for new_annotation in reversed(new_annotations):
                    self.lines.insert(annotation_lines[0], new_annotation)

                # Add imports
                self.imports_to_add.add('org.osgi.service.component.annotations.Component')
                self.imports_to_add.add('javax.servlet.Servlet')
                if paths_match or path_match:
                    self.imports_to_add.add('org.apache.sling.servlets.annotations.SlingServletPaths')
                else:
                    self.imports_to_add.add('org.apache.sling.servlets.annotations.SlingServletResourceTypes')

                self.changes_made = True
                self.stats.sling_servlets_migrated += 1
                i = annotation_lines[0] + len(new_annotations)
                continue

            i += 1

    def _migrate_sling_filter(self):
        """Migrate @SlingFilter to @Component with @SlingServletFilter annotation."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if '@SlingFilter' in line and not line.strip().startswith('//'):
                # Collect full annotation
                annotation = line
                paren_count = line.count('(') - line.count(')')
                j = i + 1
                annotation_lines = [i]

                while paren_count > 0 and j < len(self.lines):
                    annotation += ' ' + self.lines[j].strip()
                    annotation_lines.append(j)
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    j += 1

                # Parse attributes
                indent = len(self.lines[annotation_lines[0]]) - len(self.lines[annotation_lines[0]].lstrip())
                indent_str = ' ' * indent

                attrs = []

                # Scope
                scope_match = re.search(r'scope\s*=\s*SlingFilterScope\.(\w+)', annotation)
                if scope_match:
                    attrs.append(f'scope = SlingServletFilterScope.{scope_match.group(1)}')

                # Order (maps to order parameter in @SlingServletFilter)
                order_match = re.search(r'order\s*=\s*(-?\d+)', annotation)
                if order_match:
                    attrs.append(f'order = {order_match.group(1)}')

                # Pattern
                pattern_match = re.search(r'pattern\s*=\s*"([^"]+)"', annotation)
                if pattern_match:
                    attrs.append(f'pattern = "{pattern_match.group(1)}"')

                # Generate annotations
                new_annotations = []
                new_annotations.append(f'{indent_str}@Component(service = Filter.class)')

                if attrs:
                    if len(attrs) == 1:
                        new_annotations.append(f'{indent_str}@SlingServletFilter({attrs[0]})')
                    else:
                        attrs_str = ',\n    '.join(attrs)
                        new_annotations.append(
                            f'{indent_str}@SlingServletFilter(\n'
                            f'{indent_str}    {attrs_str}\n'
                            f'{indent_str})'
                        )
                else:
                    new_annotations.append(f'{indent_str}@SlingServletFilter')

                # Replace the annotation
                for idx in reversed(annotation_lines):
                    del self.lines[idx]

                # Insert new annotations in reverse order
                for new_annotation in reversed(new_annotations):
                    self.lines.insert(annotation_lines[0], new_annotation)

                # Add imports
                self.imports_to_add.add('org.osgi.service.component.annotations.Component')
                self.imports_to_add.add('javax.servlet.Filter')
                self.imports_to_add.add('org.apache.sling.servlets.annotations.SlingServletFilter')
                if scope_match:
                    self.imports_to_add.add('org.apache.sling.servlets.annotations.SlingServletFilterScope')

                self.changes_made = True
                self.stats.sling_filters_migrated += 1
                i = annotation_lines[0] + len(new_annotations)
                continue

            i += 1

    def _migrate_component_annotation(self):
        """Migrate @Component annotation from SCR to OSGi R6/R7 format."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if re.search(r'@Component\s*(\(|$)', line) and not line.strip().startswith('//'):
                # Collect multi-line annotation
                annotation_lines = [i]
                paren_count = line.count('(') - line.count(')')
                j = i + 1
                while paren_count > 0 and j < len(self.lines):
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    annotation_lines.append(j)
                    j += 1

                # Parse the annotation content
                annotation_text = '\n'.join(self.lines[idx] for idx in annotation_lines)

                # Skip if this @Component was already migrated (has OSGi R6/R7 attributes)
                # This happens when servlet/filter migrations create @Component annotations
                if 'service =' in annotation_text or 'property =' in annotation_text:
                    i = j  # Skip to after the annotation
                    continue

                # Check if there's a @Service annotation to merge
                service_class = self._find_service_class_for_component(annotation_lines[0])

                new_annotation = self._transform_component_annotation(annotation_text, service_class)

                # Get indentation for property type annotations
                indent_match = re.match(r'^(\s*)@Component', annotation_text)
                indent = indent_match.group(1) if indent_match else ''

                # Get property type annotations - these should be added even if component doesn't change
                property_type_annotations = self._get_property_type_annotations(indent)

                # Check if we need to make any changes
                has_property_type_annotations = len(property_type_annotations) > 0
                annotation_changed = new_annotation != annotation_text

                if annotation_changed or has_property_type_annotations:
                    # Replace the lines
                    for idx in reversed(annotation_lines):
                        del self.lines[idx]

                    # Insert property type annotations first, then @Component
                    insert_pos = annotation_lines[0]
                    for pta in property_type_annotations:
                        self.lines.insert(insert_pos, pta)
                        self.stats.property_type_annotations_added += 1
                        insert_pos += 1

                    self.lines.insert(insert_pos, new_annotation)
                    self.changes_made = True
                    if annotation_changed:
                        self.stats.components_migrated += 1
                    i = insert_pos + 1
                    continue

            i += 1

    def _get_property_type_annotations(self, indent: str = '') -> List[str]:
        """Get property type annotations for properties that support them."""
        annotations = []
        if self.component_properties:
            non_configurable = [p for p in self.component_properties if not (self.has_metatype and p.is_configurable)]
            for prop in non_configurable:
                if prop.is_property_type_annotation():
                    pta = prop.to_property_type_annotation()
                    if pta:
                        annotations.append(f'{indent}{pta}')
                        # Add corresponding import
                        if prop.name == 'service.ranking':
                            self.imports_to_add.add('org.osgi.service.component.propertytypes.ServiceRanking')
                        elif prop.name == 'service.description':
                            self.imports_to_add.add('org.osgi.service.component.propertytypes.ServiceDescription')
                        elif prop.name == 'service.vendor':
                            self.imports_to_add.add('org.osgi.service.component.propertytypes.ServiceVendor')
        return annotations

    def _find_service_class_for_component(self, component_idx: int) -> Optional[str]:
        """Find the @Service annotation near this @Component."""
        # Look in the next few lines for @Service
        for i in range(component_idx + 1, min(component_idx + 5, len(self.lines))):
            line = self.lines[i]
            if '@Service' in line:
                # Extract service class if specified
                service_match = re.search(r'@Service\s*\(\s*(?:value\s*=\s*)?([^)]+)\)', line)
                if service_match:
                    return service_match.group(1).strip()
                # If no explicit class, return empty to signal service registration
                return ""
        return None

    def _transform_component_annotation(self, annotation: str, service_class: Optional[str] = None) -> str:
        """Transform SCR @Component to OSGi R6/R7 format."""
        # Extract attributes
        immediate = re.search(r'immediate\s*=\s*(true|false)', annotation)
        name = re.search(r'name\s*=\s*"([^"]*)"', annotation)
        factory = re.search(r'factory\s*=\s*"([^"]*)"', annotation)

        # Build new annotation
        attrs = []

        if name:
            attrs.append(f'name = "{name.group(1)}"')
        if immediate:
            attrs.append(f'immediate = {immediate.group(1)}')
        if factory:
            attrs.append(f'factory = "{factory.group(1)}"')
        if service_class is not None:
            if service_class:
                attrs.append(f'service = {service_class}')
            # else: no explicit service class, will auto-register

        # Add properties (excluding those that will use property type annotations)
        if self.component_properties:
            non_configurable = [p for p in self.component_properties if not (self.has_metatype and p.is_configurable)]
            # Separate property type annotations from regular properties
            regular_props = [p for p in non_configurable if not p.is_property_type_annotation()]

            if regular_props:
                prop_strings = []
                for prop in regular_props:
                    prop_strings.extend(prop.to_component_property())

                if len(prop_strings) == 1:
                    attrs.append(f'property = {{ {prop_strings[0]} }}')
                else:
                    props_str = ',\n        '.join(prop_strings)
                    attrs.append(f'property = {{\n        {props_str}\n    }}')

        # Get indentation
        indent_match = re.match(r'^(\s*)@Component', annotation)
        indent = indent_match.group(1) if indent_match else ''

        if attrs:
            attrs_str = ',\n    '.join(attrs)
            return f'{indent}@Component(\n    {attrs_str}\n{indent})'
        else:
            return f'{indent}@Component'

    def _migrate_service_annotation(self):
        """Remove @Service annotations (already merged into @Component)."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if re.search(r'@Service\s*(\(|$)', line) and not line.strip().startswith('//'):
                # Track parentheses to handle multi-line @Service annotations
                paren_count = line.count('(') - line.count(')')
                lines_to_remove = [i]
                j = i + 1

                # Continue collecting lines until annotation is complete
                while paren_count > 0 and j < len(self.lines):
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    lines_to_remove.append(j)
                    j += 1

                # Remove all lines of the @Service annotation
                for idx in reversed(lines_to_remove):
                    del self.lines[idx]

                self.changes_made = True
                self.stats.services_merged += 1
                # Don't increment i since we removed lines
                continue

            i += 1

    def _migrate_reference_annotations(self):
        """Migrate @Reference annotations."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if '@Reference' in line and not line.strip().startswith('//'):
                # Collect multi-line annotation
                paren_count = line.count('(') - line.count(')')
                annotation_lines = [i]
                j = i + 1

                while paren_count > 0 and j < len(self.lines):
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    annotation_lines.append(j)
                    j += 1

                # Apply cardinality replacements to all lines
                changed = False
                for idx in annotation_lines:
                    original_line = self.lines[idx]
                    updated_line = original_line

                    # Update cardinality values
                    updated_line = re.sub(
                        r'cardinality\s*=\s*ReferenceCardinality\.OPTIONAL_UNARY',
                        'cardinality = ReferenceCardinality.OPTIONAL',
                        updated_line
                    )
                    updated_line = re.sub(
                        r'cardinality\s*=\s*ReferenceCardinality\.MANDATORY_UNARY',
                        'cardinality = ReferenceCardinality.MANDATORY',
                        updated_line
                    )
                    updated_line = re.sub(
                        r'cardinality\s*=\s*ReferenceCardinality\.OPTIONAL_MULTIPLE',
                        'cardinality = ReferenceCardinality.MULTIPLE',
                        updated_line
                    )
                    updated_line = re.sub(
                        r'cardinality\s*=\s*ReferenceCardinality\.MANDATORY_MULTIPLE',
                        'cardinality = ReferenceCardinality.AT_LEAST_ONE',
                        updated_line
                    )

                    if updated_line != original_line:
                        self.lines[idx] = updated_line
                        changed = True

                if changed:
                    self.changes_made = True
                    self.stats.references_updated += 1

                # Skip to after the annotation
                i = j
                continue

            i += 1

    def _migrate_lifecycle_annotations(self):
        """Migrate @Activate, @Deactivate, @Modified annotations."""
        # These just need import changes, annotation syntax stays the same
        pass

    def _generate_metatype_config(self):
        """Generate @Designate and config interface for metatype."""
        configurable_props = [p for p in self.component_properties if p.is_configurable]
        if not configurable_props:
            return

        # Find the class declaration
        class_idx = None
        for i, line in enumerate(self.lines):
            if re.search(rf'public\s+(?:abstract\s+)?class\s+{self.component_name}', line):
                class_idx = i
                break

        if class_idx is None:
            return

        # Find the @Component annotation
        component_idx = None
        for i in range(class_idx - 1, max(0, class_idx - 20), -1):
            if '@Component' in self.lines[i]:
                component_idx = i
                break

        if component_idx is None:
            return

        # Add @Designate annotation before @Component
        indent = len(self.lines[component_idx]) - len(self.lines[component_idx].lstrip())
        indent_str = ' ' * indent
        # Config interface should be inside the class, so add one level of indentation
        inner_indent_str = indent_str + '    '

        designate_line = f'{indent_str}@Designate(ocd = {self.component_name}.Config.class)'
        self.lines.insert(component_idx, designate_line)
        class_idx += 1  # Adjust for inserted line

        # Generate Config interface with label and description from @Component
        ocd_parts = []
        if self.component_label:
            ocd_parts.append(f'name = "{self.component_label}"')
        else:
            ocd_parts.append(f'name = "{self.component_name} Configuration"')

        if self.component_description:
            ocd_parts.append(f'description = "{self.component_description}"')

        ocd_attrs = ', '.join(ocd_parts)

        config_lines = [
            '',
            f'{inner_indent_str}@ObjectClassDefinition({ocd_attrs})',
            f'{inner_indent_str}public @interface Config {{',
        ]

        # Methods inside the Config interface need additional indentation
        method_indent = inner_indent_str + '    '
        for prop in configurable_props:
            config_lines.append(prop.to_metatype_attribute(method_indent))

        config_lines.append(f'{inner_indent_str}}}')
        config_lines.append('')

        # Insert after class opening brace
        brace_idx = None
        for i in range(class_idx, min(class_idx + 10, len(self.lines))):
            if '{' in self.lines[i]:
                brace_idx = i
                break

        if brace_idx:
            for line in reversed(config_lines):
                self.lines.insert(brace_idx + 1, line)

        # Add metatype imports
        self.imports_to_add.add('org.osgi.service.metatype.annotations.AttributeDefinition')
        self.imports_to_add.add('org.osgi.service.metatype.annotations.Designate')
        self.imports_to_add.add('org.osgi.service.metatype.annotations.ObjectClassDefinition')

        # Update lifecycle methods to use Config type instead of Map
        self._update_lifecycle_methods_to_use_config()

        self.changes_made = True

    def _update_lifecycle_methods_to_use_config(self):
        """Update @Activate and @Modified methods to use Config type instead of Map."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            # Look for @Activate or @Modified annotations
            if ('@Activate' in line or '@Modified' in line) and not line.strip().startswith('//'):
                # Find the method signature (usually on the next few lines)
                for j in range(i + 1, min(i + 5, len(self.lines))):
                    method_line = self.lines[j]

                    # Match various Map parameter patterns
                    # Map<String, Object> config
                    # Map<String,Object> properties
                    # Map<String, Object> props
                    map_pattern = r'(Map\s*<\s*String\s*,\s*Object\s*>)\s+(\w+)'
                    match = re.search(map_pattern, method_line)

                    if match:
                        # Replace Map<String, Object> with Config, preserving the parameter name
                        param_name = match.group(2)
                        new_line = re.sub(map_pattern, f'Config {param_name}', method_line)

                        # Also remove any Map imports that might become unused
                        if 'import java.util.Map' not in str(self.imports_to_remove):
                            # We'll let the import stay for now, as there might be other uses
                            pass

                        self.lines[j] = new_line
                        self.changes_made = True
                        break

            i += 1

    def _remove_property_annotations(self):
        """Remove @Property annotations after migration."""
        result = []
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            # Check if this is a @Property annotation (but not commented)
            if '@Property' in line and not line.strip().startswith('//'):
                # This is the start of a @Property annotation - need to remove it
                # Track parentheses to find where the annotation ends
                paren_count = line.count('(') - line.count(')')
                i += 1

                # Continue removing lines until annotation is complete
                while paren_count > 0 and i < len(self.lines):
                    paren_count += self.lines[i].count('(') - self.lines[i].count(')')
                    i += 1

                # Now i points to the line after the complete annotation
                continue

            # Keep this line
            result.append(line)
            i += 1

        self.lines = result

    def _update_imports(self):
        """Update import statements."""
        # Remove old imports
        self.lines = [
            line for line in self.lines
            if not any(f'import {imp}' in line for imp in self.imports_to_remove)
        ]

        # Remove property annotations
        self._remove_property_annotations()

        # Add new imports after the last import or package statement
        last_import_idx = -1
        for i, line in enumerate(self.lines):
            if line.strip().startswith('import ') or line.strip().startswith('package '):
                last_import_idx = i

        if last_import_idx >= 0 and self.imports_to_add:
            # Find the insertion point (after last import)
            insert_idx = last_import_idx + 1

            # Add a blank line before new imports if not present
            if insert_idx < len(self.lines) and self.lines[insert_idx].strip():
                self.lines.insert(insert_idx, '')
                insert_idx += 1

            for new_import in sorted(self.imports_to_add):
                self.lines.insert(insert_idx, f'import {new_import};')
                insert_idx += 1


def migrate_file(file_path: Path, dry_run: bool = False, stats: MigrationStats = None) -> bool:
    """Migrate a single Java file."""
    if stats is None:
        stats = MigrationStats()

    stats.files_processed += 1

    try:
        content = file_path.read_text(encoding='utf-8')
        migrator = AnnotationMigrator(content, file_path, stats)
        new_content, changed = migrator.migrate()

        if changed:
            if not dry_run:
                file_path.write_text(new_content, encoding='utf-8')

            stats.files_changed += 1
            print(f"{'[DRY RUN] Would migrate' if dry_run else 'Migrated'}: {file_path}")
            return True
        else:
            print(f"No changes needed: {file_path}")
            return False
    except Exception as e:
        stats.add_error(file_path, str(e))
        print(f"Error processing {file_path}: {e}", file=sys.stderr)
        return False


def find_java_files(directory: Path) -> List[Path]:
    """Find all Java files in the directory."""
    return list(directory.rglob('*.java'))


def validate_migrated_code(file_path: Path, stats: MigrationStats) -> bool:
    """Validate migrated code for common issues."""
    try:
        content = file_path.read_text(encoding='utf-8')

        # Check for remaining SCR imports
        if 'org.apache.felix.scr.annotations' in content:
            stats.add_warning(file_path, "Still contains Felix SCR imports")

        # Check for @Property annotations
        if re.search(r'@Property\s*\(', content):
            stats.add_warning(file_path, "Still contains @Property annotations")

        # Check for @Service annotations
        if re.search(r'@Service\s*(\(|$)', content) and 'org.apache.felix.scr.annotations.Service' not in content:
            # This is okay - might be from another package
            pass

        return True
    except Exception as e:
        stats.add_error(file_path, f"Validation error: {e}")
        return False


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description='Migrate Java files from Felix SCR to OSGi R6/R7 annotations (Enhanced)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s /path/to/src                    # Migrate all Java files
  %(prog)s /path/to/src --dry-run          # Preview changes
  %(prog)s /path/to/MyClass.java           # Migrate single file
  %(prog)s /path/to/src --validate         # Validate after migration
        """
    )
    parser.add_argument(
        'path',
        type=Path,
        help='Path to Java file or directory containing Java files'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be changed without modifying files'
    )
    parser.add_argument(
        '--validate',
        action='store_true',
        help='Validate migrated code for common issues'
    )

    args = parser.parse_args()

    if not args.path.exists():
        print(f"Error: Path does not exist: {args.path}", file=sys.stderr)
        sys.exit(1)

    # Find files to process
    if args.path.is_file():
        files = [args.path]
    else:
        files = find_java_files(args.path)

    print(f"Found {len(files)} Java files to process")
    print("=" * 70)

    # Process each file
    stats = MigrationStats()
    for file_path in files:
        migrate_file(file_path, args.dry_run, stats)
        if args.validate and not args.dry_run:
            validate_migrated_code(file_path, stats)

    # Print summary
    stats.print_summary()

    # Exit with error code if there were errors
    if stats.errors:
        sys.exit(1)


if __name__ == '__main__':
    main()
