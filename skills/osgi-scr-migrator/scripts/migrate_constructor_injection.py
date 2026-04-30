#!/usr/bin/env python3
"""
OSGi Constructor Injection Migration Script

Migrates OSGi components to use constructor injection for:
1. Configuration (Config parameters in @Activate methods)
2. Mandatory static unary @Reference fields

This follows OSGi R7/R8 best practices for immutable dependencies.

Features:
- Converts @Activate(Config config) to constructor injection
- Converts mandatory static unary @Reference fields to constructor parameters
- Makes eligible fields `final` for immutability
- Preserves optional/dynamic/multiple references as field injection
- Validation and comprehensive reporting
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple, Set, Dict, Optional
from dataclasses import dataclass, field
from enum import Enum


class ReferenceCardinality(Enum):
    """OSGi reference cardinality values."""
    MANDATORY = "MANDATORY"  # Default
    OPTIONAL = "OPTIONAL"
    MULTIPLE = "MULTIPLE"
    AT_LEAST_ONE = "AT_LEAST_ONE"


class ReferencePolicy(Enum):
    """OSGi reference policy values."""
    STATIC = "STATIC"  # Default
    DYNAMIC = "DYNAMIC"


@dataclass
class Reference:
    """Represents a component reference."""
    field_name: str
    field_type: str
    line_idx: int
    annotation_start_idx: int
    annotation_end_idx: int
    cardinality: Optional[ReferenceCardinality] = None
    policy: Optional[ReferencePolicy] = None
    target: Optional[str] = None
    has_bind_method: bool = False
    has_unbind_method: bool = False
    is_collection: bool = False  # List, Set, Collection

    def is_eligible_for_constructor_injection(self) -> bool:
        """Check if this reference can be moved to constructor."""
        # Must be mandatory (default or explicit MANDATORY)
        if self.cardinality == ReferenceCardinality.OPTIONAL:
            return False
        if self.cardinality == ReferenceCardinality.MULTIPLE:
            return False
        if self.cardinality == ReferenceCardinality.AT_LEAST_ONE:
            return False

        # Must be static (default or explicit STATIC)
        if self.policy == ReferencePolicy.DYNAMIC:
            return False

        # Must not have custom bind/unbind methods
        if self.has_bind_method or self.has_unbind_method:
            return False

        # Must not be a collection
        if self.is_collection:
            return False

        return True


@dataclass
class ActivateMethod:
    """Represents an @Activate method."""
    method_start_idx: int
    method_end_idx: int
    has_config_param: bool = False
    config_type: Optional[str] = None
    config_param_name: Optional[str] = None
    has_other_params: bool = False


@dataclass
class ConstructorInjectionStats:
    """Track migration statistics."""
    files_processed: int = 0
    files_changed: int = 0
    components_migrated: int = 0
    configs_converted: int = 0
    references_converted: int = 0
    fields_made_final: int = 0
    errors: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)
    skipped: List[str] = field(default_factory=list)

    def add_error(self, file_path: Path, message: str):
        """Add an error message."""
        self.errors.append(f"{file_path}: {message}")

    def add_warning(self, file_path: Path, message: str):
        """Add a warning message."""
        self.warnings.append(f"{file_path}: {message}")

    def add_skipped(self, file_path: Path, message: str):
        """Add a skipped message."""
        self.skipped.append(f"{file_path}: {message}")

    def print_summary(self):
        """Print migration summary."""
        print("\n" + "=" * 70)
        print("CONSTRUCTOR INJECTION MIGRATION SUMMARY")
        print("=" * 70)
        print(f"Files processed:              {self.files_processed}")
        print(f"Files changed:                {self.files_changed}")
        print(f"Components migrated:          {self.components_migrated}")
        print(f"Configs converted:            {self.configs_converted}")
        print(f"References converted:         {self.references_converted}")
        print(f"Fields made final:            {self.fields_made_final}")

        if self.skipped:
            print(f"\nSkipped: {len(self.skipped)}")
            for skip in self.skipped[:10]:
                print(f"  ⊘ {skip}")
            if len(self.skipped) > 10:
                print(f"  ... and {len(self.skipped) - 10} more")

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


class ConstructorInjectionMigrator:
    def __init__(self, content: str, file_path: Path, stats: ConstructorInjectionStats):
        self.content = content
        self.file_path = file_path
        self.lines = content.split('\n')
        self.stats = stats
        self.changes_made = False
        self.component_name = ""
        self.has_component = False

    def migrate(self) -> Tuple[str, bool]:
        """Migrate component to use constructor injection."""
        try:
            self._extract_class_info()

            if not self.has_component:
                self.stats.add_skipped(self.file_path, "No @Component annotation found")
                return self.content, False

            # Find @Activate method
            activate_method = self._find_activate_method()
            if not activate_method:
                self.stats.add_skipped(self.file_path, "No @Activate method found")
                return '\n'.join(self.lines), self.changes_made

            # Find eligible references
            references = self._find_references()
            eligible_refs = [ref for ref in references if ref.is_eligible_for_constructor_injection()]

            # Check if there's anything to migrate
            if not activate_method.has_config_param and not eligible_refs:
                self.stats.add_skipped(
                    self.file_path,
                    "No configuration or eligible references for constructor injection"
                )
                return '\n'.join(self.lines), self.changes_made

            # Perform migration
            self._migrate_to_constructor_injection(activate_method, eligible_refs)

            if self.changes_made:
                self.stats.components_migrated += 1

            return '\n'.join(self.lines), self.changes_made

        except Exception as e:
            self.stats.add_error(self.file_path, str(e))
            return self.content, False

    def _extract_class_info(self):
        """Extract class name and check for @Component."""
        for i, line in enumerate(self.lines):
            # Check for @Component
            if '@Component' in line and not line.strip().startswith('//'):
                self.has_component = True

            # Extract class name
            match = re.search(r'public\s+(?:abstract\s+)?class\s+(\w+)', line)
            if match:
                self.component_name = match.group(1)

    def _find_activate_method(self) -> Optional[ActivateMethod]:
        """Find @Activate method and analyze its parameters."""
        i = 0
        while i < len(self.lines):
            line = self.lines[i]

            if '@Activate' in line and not line.strip().startswith('//'):
                # Find method signature (usually next few lines)
                for j in range(i + 1, min(i + 10, len(self.lines))):
                    method_line = self.lines[j]

                    # Match method signature patterns
                    # Examples:
                    # private void activate(Config config)
                    # protected void activate(Config config) {
                    # void activate(final Config config)
                    method_match = re.search(
                        r'(public|protected|private)?\s*(void|protected|private)?\s+(\w+)\s*\((.*?)\)',
                        method_line
                    )

                    if method_match:
                        params_str = method_match.group(4).strip()

                        # Find method body end
                        method_end = self._find_method_end(j)

                        activate = ActivateMethod(
                            method_start_idx=i,  # @Activate line
                            method_end_idx=method_end
                        )

                        # Parse parameters
                        if params_str:
                            # Look for Config parameter
                            # Match patterns like: Config config, final Config cfg, MyService.Config config
                            # Pattern matches: optional "final", then anything ending with "Config", then parameter name
                            config_match = re.search(
                                r'(?:final\s+)?((?:[\w.]+\.)?Config)\s+(\w+)',
                                params_str
                            )

                            if config_match:
                                activate.has_config_param = True
                                activate.config_type = config_match.group(1)
                                activate.config_param_name = config_match.group(2)

                            # Check for other parameters (BundleContext, ComponentContext, etc.)
                            # We'll keep these in the constructor
                            if ',' in params_str or (params_str and not config_match):
                                activate.has_other_params = True

                        return activate

                break
            i += 1

        return None

    def _find_method_end(self, method_start_idx: int) -> int:
        """Find the end of a method by tracking braces."""
        brace_count = 0
        found_opening = False

        for i in range(method_start_idx, len(self.lines)):
            line = self.lines[i]

            for char in line:
                if char == '{':
                    brace_count += 1
                    found_opening = True
                elif char == '}':
                    brace_count -= 1
                    if found_opening and brace_count == 0:
                        return i

        return method_start_idx

    def _find_references(self) -> List[Reference]:
        """Find all @Reference field annotations."""
        references = []
        i = 0

        while i < len(self.lines):
            line = self.lines[i]

            if '@Reference' in line and not line.strip().startswith('//'):
                # Collect full annotation (may span multiple lines)
                annotation_start = i
                annotation_text = line
                paren_count = line.count('(') - line.count(')')
                j = i + 1

                while paren_count > 0 and j < len(self.lines):
                    annotation_text += ' ' + self.lines[j].strip()
                    paren_count += self.lines[j].count('(') - self.lines[j].count(')')
                    j += 1

                annotation_end = j - 1

                # Find the field declaration (should be next non-empty line)
                field_idx = j
                while field_idx < len(self.lines) and not self.lines[field_idx].strip():
                    field_idx += 1

                if field_idx < len(self.lines):
                    field_line = self.lines[field_idx]

                    # Parse field: private/protected/package-private Type fieldName;
                    # Handle patterns like:
                    # private ResourceResolverFactory resolverFactory;
                    # protected final SlingRepository repository;
                    # private List<MyService> services;
                    field_match = re.search(
                        r'^\s*(?:private|protected|public)?\s*(?:static)?\s*(?:final)?\s*([A-Za-z_][\w.<>,\s]*?)\s+(\w+)\s*[;=]',
                        field_line
                    )

                    if field_match:
                        field_type = field_match.group(1).strip()
                        field_name = field_match.group(2)

                        # Parse reference attributes
                        ref = Reference(
                            field_name=field_name,
                            field_type=field_type,
                            line_idx=field_idx,
                            annotation_start_idx=annotation_start,
                            annotation_end_idx=annotation_end
                        )

                        # Check for cardinality
                        card_match = re.search(r'cardinality\s*=\s*ReferenceCardinality\.(\w+)', annotation_text)
                        if card_match:
                            try:
                                ref.cardinality = ReferenceCardinality[card_match.group(1)]
                            except KeyError:
                                pass

                        # Check for policy
                        policy_match = re.search(r'policy\s*=\s*ReferencePolicy\.(\w+)', annotation_text)
                        if policy_match:
                            try:
                                ref.policy = ReferencePolicy[policy_match.group(1)]
                            except KeyError:
                                pass

                        # Check for target
                        target_match = re.search(r'target\s*=\s*"([^"]+)"', annotation_text)
                        if target_match:
                            ref.target = target_match.group(1)

                        # Check for bind/unbind methods
                        if 'bind' in annotation_text.lower():
                            ref.has_bind_method = True
                        if 'unbind' in annotation_text.lower():
                            ref.has_unbind_method = True

                        # Check if field is a collection
                        if re.match(r'(List|Set|Collection)<', field_type):
                            ref.is_collection = True

                        references.append(ref)

                i = field_idx + 1
                continue

            i += 1

        return references

    def _migrate_to_constructor_injection(
        self,
        activate_method: ActivateMethod,
        eligible_refs: List[Reference]
    ):
        """Transform component to use constructor injection."""

        # Build constructor parameters
        constructor_params = []

        # Config parameter comes first (if present)
        if activate_method.has_config_param:
            constructor_params.append(
                f'{activate_method.config_type} {activate_method.config_param_name}'
            )
            self.stats.configs_converted += 1

        # Add eligible references
        for ref in eligible_refs:
            # Build @Reference annotation for parameter
            ref_attrs = []
            if ref.target:
                ref_attrs.append(f'target = "{ref.target}"')

            if ref_attrs:
                ref_annotation = f'@Reference({", ".join(ref_attrs)})'
            else:
                ref_annotation = '@Reference'

            constructor_params.append(
                f'{ref_annotation} {ref.field_type} {ref.field_name}'
            )
            self.stats.references_converted += 1

        # Find class body start
        class_idx = None
        for i, line in enumerate(self.lines):
            if re.search(rf'public\s+(?:abstract\s+)?class\s+{self.component_name}', line):
                class_idx = i
                break

        if class_idx is None:
            return

        # Find opening brace of class
        brace_idx = None
        for i in range(class_idx, min(class_idx + 10, len(self.lines))):
            if '{' in self.lines[i]:
                brace_idx = i
                break

        if brace_idx is None:
            return

        # IMPORTANT: Extract data BEFORE removing anything!

        # Extract config field assignments before removing @Activate method
        config_field_assignments = []
        if activate_method.has_config_param:
            config_field_assignments = self._extract_config_field_assignments(activate_method)
            # Make config fields final before any removal
            self._make_config_fields_final(activate_method)

        # Make eligible reference fields final before removal
        for ref in eligible_refs:
            field_line = self.lines[ref.line_idx]
            if 'final' not in field_line:
                # Insert 'final' after visibility modifier or at start
                updated_line = re.sub(
                    r'((private|protected)\s+)',
                    r'\1final ',
                    field_line
                )
                if updated_line == field_line:
                    # No visibility modifier found, add before type
                    updated_line = re.sub(
                        rf'({ref.field_type}\s+{ref.field_name})',
                        rf'final \1',
                        field_line
                    )

                if updated_line != field_line:
                    self.lines[ref.line_idx] = updated_line
                    self.stats.fields_made_final += 1
                    self.changes_made = True

        # NOW remove old code (after extracting what we need)

        # Remove old @Activate method
        self._remove_activate_method(activate_method)

        # Remove @Reference annotations from fields that were moved to constructor
        # Sort by annotation_start_idx descending to remove from bottom to top
        for ref in sorted(eligible_refs, key=lambda r: r.annotation_start_idx, reverse=True):
            self._remove_reference_annotation(ref)

        # NOW generate and insert constructor (after removals, so indices are stable)
        # Need to recalculate brace_idx since we removed lines
        class_idx = None
        for i, line in enumerate(self.lines):
            if re.search(rf'public\s+(?:abstract\s+)?class\s+{self.component_name}', line):
                class_idx = i
                break

        if class_idx is None:
            return

        # Find opening brace of class
        brace_idx = None
        for i in range(class_idx, min(class_idx + 10, len(self.lines))):
            if '{' in self.lines[i]:
                brace_idx = i
                break

        if brace_idx is None:
            return

        indent = '    '  # Assume 4-space indent
        constructor_lines = []
        constructor_lines.append('')
        constructor_lines.append(f'{indent}@Activate')
        constructor_lines.append(f'{indent}public {self.component_name}(')

        # Format parameters (one per line for readability)
        for i, param in enumerate(constructor_params):
            suffix = ',' if i < len(constructor_params) - 1 else ''
            constructor_lines.append(f'{indent}    {param}{suffix}')

        constructor_lines.append(f'{indent}) {{')

        # Add assignments for config fields (using pre-extracted assignments)
        for field_assignment in config_field_assignments:
            constructor_lines.append(f'{indent}    {field_assignment}')

        # Add assignments for references
        for ref in eligible_refs:
            constructor_lines.append(f'{indent}    this.{ref.field_name} = {ref.field_name};')

        constructor_lines.append(f'{indent}}}')
        constructor_lines.append('')

        # Insert constructor after class opening brace
        insert_pos = brace_idx + 1
        for line in reversed(constructor_lines):
            self.lines.insert(insert_pos, line)

        self.changes_made = True

    def _make_config_fields_final(self, activate_method: ActivateMethod):
        """Make fields assigned from config parameter final."""
        # Find fields assigned in the activate method
        for i in range(activate_method.method_start_idx, activate_method.method_end_idx + 1):
            line = self.lines[i]

            # Look for patterns like: this.timeout = config.timeout();
            assign_match = re.search(r'this\.(\w+)\s*=\s*' + re.escape(activate_method.config_param_name or 'config'), line)
            if assign_match:
                field_name = assign_match.group(1)

                # Find the field declaration
                for j, field_line in enumerate(self.lines):
                    if re.search(rf'\s+{field_name}\s*[;=]', field_line) and 'final' not in field_line:
                        # Add final modifier
                        updated_line = re.sub(
                            r'((private|protected)\s+)',
                            r'\1final ',
                            field_line
                        )
                        if updated_line != field_line:
                            self.lines[j] = updated_line
                            self.stats.fields_made_final += 1
                            self.changes_made = True
                        break

    def _extract_config_field_assignments(self, activate_method: ActivateMethod) -> List[str]:
        """Extract field assignments from activate method body."""
        assignments = []
        config_param = activate_method.config_param_name or 'config'

        for i in range(activate_method.method_start_idx, activate_method.method_end_idx + 1):
            line = self.lines[i].strip()

            # Look for patterns like: this.timeout = config.timeout();
            # or: this.maxRetries = config.maxRetries();
            assign_match = re.search(rf'this\.(\w+)\s*=\s*{config_param}\.(\w+)\(\)\s*;', line)
            if assign_match:
                field_name = assign_match.group(1)
                config_method = assign_match.group(2)
                assignments.append(f'this.{field_name} = {config_param}.{config_method}();')

        return assignments

    def _remove_activate_method(self, activate_method: ActivateMethod):
        """Remove the old @Activate method."""
        # Remove from @Activate annotation to end of method
        for i in range(activate_method.method_end_idx, activate_method.method_start_idx - 1, -1):
            del self.lines[i]

        # Remove any blank lines before where the method was
        idx = activate_method.method_start_idx - 1
        while idx >= 0 and not self.lines[idx].strip():
            del self.lines[idx]
            idx -= 1

    def _remove_reference_annotation(self, ref: Reference):
        """Remove @Reference annotation from field (keeping the field declaration)."""
        # Remove annotation lines (from end to start to avoid index issues)
        for i in range(ref.annotation_end_idx, ref.annotation_start_idx - 1, -1):
            del self.lines[i]


def migrate_file(
    file_path: Path,
    dry_run: bool = False,
    stats: ConstructorInjectionStats = None
) -> bool:
    """Migrate a single Java file to use constructor injection."""
    if stats is None:
        stats = ConstructorInjectionStats()

    stats.files_processed += 1

    try:
        content = file_path.read_text(encoding='utf-8')
        migrator = ConstructorInjectionMigrator(content, file_path, stats)
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


def validate_migrated_code(file_path: Path, stats: ConstructorInjectionStats) -> bool:
    """Validate migrated code for common issues."""
    try:
        content = file_path.read_text(encoding='utf-8')

        # Check for @Activate methods that still take Config parameters
        if re.search(r'@Activate.*?Config\s+\w+', content, re.DOTALL):
            stats.add_warning(
                file_path,
                "Still has @Activate method with Config parameter - may need manual review"
            )

        # Check for non-final fields that could be final
        lines = content.split('\n')
        for i, line in enumerate(lines):
            if '@Reference' not in line and 'private' in line and 'final' not in line:
                # This is a private field without final - might be a candidate
                # Only warn if it's not being modified elsewhere
                if re.search(r'private\s+\w+\s+\w+\s*[;=]', line):
                    pass  # Could add more sophisticated checking here

        return True
    except Exception as e:
        stats.add_error(file_path, f"Validation error: {e}")
        return False


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description='Migrate OSGi components to use constructor injection (Step 7 optimization)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
This script implements Step 7 of the OSGi SCR migration: optimizing components
to use constructor injection for configurations and mandatory static unary references.

Benefits:
  • Immutable dependencies (fields can be final)
  • Explicit dependencies (visible in constructor signature)
  • Better testability (easier to mock)
  • Thread safety (immutable references)

Examples:
  %(prog)s /path/to/src                    # Migrate all Java files
  %(prog)s /path/to/src --dry-run          # Preview changes
  %(prog)s /path/to/MyService.java         # Migrate single file
  %(prog)s /path/to/src --validate         # Validate after migration

Prerequisites:
  • Run migrate_annotations.py first (Step 2)
  • All tests must pass before running this optimization
  • Commit code before running (for easy rollback)
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
    stats = ConstructorInjectionStats()
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
