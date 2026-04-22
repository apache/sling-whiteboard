# Apache Sling AI tooling evaluation

This repository contains [Inspect](https://inspect.aisi.org.uk/)-based evals for Apache Sling agent workflows.

## Repository purpose

- evaluates agent behavior on concrete Sling maintenance tasks
- compares runs with and without Inspect skills
- uses Docker sandboxes for task execution and scoring

## Current layout

- Python project managed with `uv`
- Inspect tasks live under `src/skill_evals/`
- task registration is provided by `src/skill_evals/_registry.py`
- task-specific assets live alongside each task package, for example:
  - `task.py`
  - `scorer.py`
  - `dataset.jsonl`
  - `setup.sh`
  - `Dockerfile`
  - `compose.yaml`

## Current task set

- the repository currently includes the `jcr_js_nodetypes` eval task
- this task evaluates updating the Sling parent POM in `org-apache-sling-jcr-js-nodetypes`
- samples can run either with skill installation enabled or disabled

## Skills

- skills are sourced from `../skills`
- only directories containing `SKILL.md` are exposed as Inspect skills
- when enabled for a sample, skills are installed inside the sandbox at `/workspace/skills`

## Sandbox model

- the task uses a Docker sandbox defined by a task-local `Dockerfile` and `compose.yaml`
- the runtime image includes Maven 3 and Java 8
- the target repository is cloned into the sandbox image
- a `mvn8` wrapper is provided inside the sandbox for Java 8 Maven usage

## Evaluation model

- sample setup is handled by a shared shell script copied into the sandbox
- scoring validates repository state inside the sandbox rather than relying on model text output
- the current scorer checks both the parent POM version change and whether `mvn clean verify` succeeds
