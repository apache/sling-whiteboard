import re

from inspect_ai.scorer import Score, Scorer, accuracy, scorer, stderr
from inspect_ai.solver import TaskState
from inspect_ai.util import sandbox


REPO_DIR = "/workspace/sling-org-apache-sling-jcr-js-nodetypes"
POM_PATH = f"{REPO_DIR}/pom.xml"
PARENT_VERSION_RE = re.compile(r"<parent>.*?<version>([^<]+)</version>", re.DOTALL)
REQUIRED_PARENT_POM_27_DEPENDENCIES = (
    "org.osgi:osgi.cmpn:jar:6.0.0:provided",
    "org.osgi:osgi.core:jar:6.0.0:provided",
    "javax.servlet:javax.servlet-api:jar:3.1.0:provided",
)


async def check_parent_pom_27_dependencies() -> tuple[int, str]:
    dependency_result = await sandbox().exec(
        ["mvn", "dependency:list"],
        cwd=REPO_DIR,
        timeout=600,
        timeout_retry=False,
    )
    dependency_output = "\n".join(
        part for part in [dependency_result.stdout, dependency_result.stderr] if part
    )
    missing_dependencies = [
        dependency
        for dependency in REQUIRED_PARENT_POM_27_DEPENDENCIES
        if dependency not in dependency_output
    ]
    dependency_check_passed = 1 if dependency_result.success and not missing_dependencies else 0
    parent_pom_27_dependency_check = (
        "passed"
        if dependency_check_passed
        else f"missing: {', '.join(missing_dependencies)}"
    )
    return dependency_check_passed, parent_pom_27_dependency_check


@scorer(metrics=[accuracy(), stderr()])
def parent_pom_update() -> Scorer:
    async def score(state: TaskState, target) -> Score:
        expected_version = None
        if state.metadata:
            expected_version = state.metadata.get("expected_parent_version")

        pom_contents = await sandbox().read_file(POM_PATH)
        match = PARENT_VERSION_RE.search(pom_contents)
        actual_version = match.group(1).strip() if match else None

        build_result = await sandbox().exec(
            ["mvn", "clean", "verify"],
            cwd=REPO_DIR,
            timeout=600,
            timeout_retry=False,
        )

        dependency_check_passed = 1
        parent_pom_27_dependency_check = None
        if expected_version == "27":
            (
                dependency_check_passed,
                parent_pom_27_dependency_check,
            ) = await check_parent_pom_27_dependencies()

        parent_version_correct = 1 if actual_version == expected_version else 0
        build_passed = 1 if build_result.success else 0
        overall = 1 if parent_version_correct and build_passed and dependency_check_passed else 0

        stderr_tail = build_result.stderr.strip()
        if stderr_tail:
            stderr_tail = stderr_tail[-2000:]

        explanation = (
            f"Expected parent version: {expected_version}\n"
            f"Actual parent version: {actual_version}\n"
            f"mvn clean verify success: {build_result.success}\n"
            f"mvn clean verify returncode: {build_result.returncode}\n"
        )
        if stderr_tail:
            explanation += f"mvn stderr (tail):\n{stderr_tail}\n"

        metadata = {
            "expected_parent_version": expected_version,
            "actual_parent_version": actual_version,
            "mvn_returncode": build_result.returncode,
            "parent_version_correct": parent_version_correct,
            "build_passed": build_passed,
            "overall": overall,
        }
        if parent_pom_27_dependency_check is not None:
            metadata["parent_pom_27_dependency_check"] = parent_pom_27_dependency_check

        return Score(
            value=overall,
            answer=f"parent_version={actual_version}, build_success={build_result.success}",
            explanation=explanation,
            metadata=metadata,
        )

    return score
