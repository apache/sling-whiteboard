# Apache Sling Skill Evals

Minimal [Inspect](https://inspect.aisi.org.uk/) eval scaffolding for Apache Sling agent workflows.

Tests updating the parent pom for the `org-apache-sling-jcr-js-nodetypes` repository with or without the `update-sling-parent-pom` skill.

## Requirements

- `uv`
- Docker
- Credentials for [Model Providers supported by Inspect](https://inspect.aisi.org.uk/providers.html).

## Setup

This project requires Python 3.12 or newer. `uv` will use a compatible system Python when one is
available, or install a managed Python if needed.

```bash
uv sync
```

### OpenRouter

```bash
export OPENROUTER_API_KEY=your-openrouter-api-key

export INSPECT_EVAL_MODEL=openrouter/openai/gpt-oss-120b:free
```

### AWS Bedrock

```bash
export AWS_DEFAULT_REGION=your-region # e.g. us-east-1

# option 1- access key
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key

# option 2 - bearer token
export AWS_BEARER_TOKEN=your-bearer-token

export INSPECT_EVAL_MODEL=bedrock/us.anthropic.claude-sonnet-4-5-20250929-v1:0
```

## Execution

```bash
uv run inspect eval skill_evals/jcr_js_nodetypes
```

By default the task runs without skills because `skill_enabled` defaults to `false`.

Enable or disable skills explicitly with a task argument:

```bash
uv run inspect eval skill_evals/jcr_js_nodetypes -T skill_enabled=true
uv run inspect eval skill_evals/jcr_js_nodetypes -T skill_enabled=false
```

You can also set the task argument through Inspect's standard environment variable:

```bash
export INSPECT_EVAL_TASK_ARGS=skill_enabled=true
uv run inspect eval skill_evals/jcr_js_nodetypes
```

You can also specify the desired model from the command line:

```bash
uv run inspect eval skill_evals/jcr_js_nodetypes \
    --model my-model
```

Logs can be inspected afterwards with
```bash
uv run inspect view
```

## Task Notes

Current status:

- The sandbox image includes Maven 3, Java 8, the target repository, and a `mvn8` wrapper.
- The sample setup script checks out git revision `52cea70b190c8389495a4fa494161eac13416891`, where the parent POM version is `22`.
- The prompt used for the sample is `update to the next parent pom version`.
- A run is considered successful only if:
  - the parent POM version is updated to `23`
  - `mvn clean verify` succeeds

Extension plans:

- add more tasks with different parent pom versions in the same repository
- add missing mvn wrappers ( mvn11, mvn17, mvn21 ) OR update the skill to be able to use different mechanisms
  for discovering maven with different versions ( e.g. update-alternatives, sdkman )
- investigate sharing the local Maven repository to speed up evaluation
- add more scenarios around problematic parent pom updates - 27 ( OSGi R6), 31 ( official OSGi annotations ) and 35 (switch to bundle-parent)
