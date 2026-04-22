from pathlib import Path

from inspect_ai import Task, task
from inspect_ai.agent import react
from inspect_ai.dataset import Sample, json_dataset
from inspect_ai.solver import solver
from inspect_ai.tool import bash, skill, text_editor

from .scorer import parent_pom_update


def _task_dir() -> Path:
    return Path(__file__).resolve().parent


def _skills_dir() -> Path:
    return _task_dir().parents[3] / "skills"


def _skill_paths() -> list[Path]:
    skills_dir = _skills_dir()
    return sorted(path for path in skills_dir.iterdir() if (path / "SKILL.md").is_file())


def _record_to_sample(record: dict, *, skill_enabled: bool) -> Sample:
    metadata = {
        "skill_enabled": skill_enabled,
        "git_revision": record["git_revision"],
        "expected_parent_version": record["expected_parent_version"],
    }
    sample_id = record.get("id", "upgrade-parent-pom-{expected_parent_version}").format(
        **metadata
    )

    return Sample(
        id=sample_id,
        input=record["input"].format(**metadata),
        metadata=metadata,
        files={"/workspace/eval-assets/setup.sh": str(_task_dir() / "setup.sh")},
        setup=f"sh /workspace/eval-assets/setup.sh {record['git_revision']}",
    )


@task
def jcr_js_nodetypes(skill_enabled: bool = False) -> Task:
    dataset = json_dataset(
        str(_task_dir() / "dataset.jsonl"),
        sample_fields=lambda record: _record_to_sample(
            record, skill_enabled=skill_enabled
        ),
    )

    skill_paths = _skill_paths()
    task_name_suffix = "skills" if skill_enabled else "no skills"

    @solver
    def sample_solver():
        async def solve(state, generate):
            tools = [
                bash(timeout=600),
                text_editor(timeout=600),
            ]
            if state.metadata and state.metadata.get("skill_enabled") and skill_paths:
                tools.append(skill(skill_paths, dir="/workspace/skills"))

            agent = react(
                prompt=(
                    "You are working in /workspace/sling-org-apache-sling-jcr-js-nodetypes. "
                    "Use available tools to inspect and modify the repository as needed. "
                    "Run Maven builds to validate your changes."
                ),
                tools=tools,
            )
            return await agent(state)

        return solve

    return Task(
        name=f"jcr_js_nodetypes [{task_name_suffix}]",
        dataset=dataset,
        solver=sample_solver(),
        scorer=parent_pom_update(),
        sandbox=("docker", str(_task_dir() / "compose.yaml")),
        metadata={"skill_enabled": skill_enabled},
        epochs=5,
        time_limit=600,
    )
