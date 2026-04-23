from __future__ import annotations

import json
import math
from functools import lru_cache
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from urllib.error import URLError
from urllib.request import Request, urlopen
from typing import Any

from inspect_ai.log import read_eval_log, read_eval_log_sample_summaries


JsonValue = dict[str, Any] | list[Any] | str | int | float | bool | None

MODELS_DEV_API_URL = "https://models.dev/api.json"
MODELS_DEV_PROVIDER_ALIASES = {"bedrock": "amazon-bedrock"}
MODELS_DEV_CACHE_PATH = Path(__file__).resolve().parents[2] / ".cache" / "models.dev-api.json"


EXPECTED_DIFF_GROUPS: dict[str, set[str]] = {
    "skill_enabled": {"skill_enabled", "task_args", "metadata", "task"},
    "model": {"model", "model_generate_config"},
    "task": {"task"},
    "task_args": {"task_args"},
    "metadata": {"metadata"},
    "config": {"config"},
    "model_generate_config": {"model_generate_config"},
}


@dataclass(frozen=True)
class CoordinateDiff:
    coordinate: str
    left: JsonValue
    right: JsonValue


@dataclass(frozen=True)
class CostComputation:
    total_cost: float | None
    warnings: list[str]


@dataclass(frozen=True)
class PricingData:
    model_costs: dict[str, dict[str, float]]
    source: str


def _expand_expected_diff(expected_diff: set[str]) -> set[str]:
    expanded: set[str] = set()
    for item in expected_diff:
        expanded.update(EXPECTED_DIFF_GROUPS.get(item, {item}))
    return expanded


def _parse_time(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value)


def _round_number(value: Any, digits: int = 6) -> Any:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return value
    if isinstance(value, float) and (math.isnan(value) or math.isinf(value)):
        return value
    return round(value, digits)


def _normalize(value: Any) -> JsonValue:
    if hasattr(value, "model_dump"):
        return _normalize(value.model_dump())
    if isinstance(value, dict):
        return {
            str(key): _normalize(val)
            for key, val in sorted(value.items(), key=lambda item: str(item[0]))
        }
    if isinstance(value, list):
        return [_normalize(item) for item in value]
    if isinstance(value, tuple):
        return [_normalize(item) for item in value]
    return _round_number(value)


def _deep_equal(left: Any, right: Any) -> bool:
    return _normalize(left) == _normalize(right)


def _diff_value(left: Any, right: Any) -> tuple[JsonValue, JsonValue] | None:
    left_normalized = _normalize(left)
    right_normalized = _normalize(right)
    if left_normalized == right_normalized:
        return None

    if isinstance(left_normalized, dict) and isinstance(right_normalized, dict):
        left_diff: dict[str, JsonValue] = {}
        right_diff: dict[str, JsonValue] = {}
        for key in sorted(set(left_normalized) | set(right_normalized)):
            child_diff = _diff_value(left_normalized.get(key), right_normalized.get(key))
            if child_diff is None:
                continue
            left_child, right_child = child_diff
            left_diff[key] = left_child
            right_diff[key] = right_child
        if not left_diff and not right_diff:
            return None
        return left_diff, right_diff

    return left_normalized, right_normalized


def _safe_getattr(obj: Any, name: str, default: Any = None) -> Any:
    return getattr(obj, name, default) if obj is not None else default


def _compute_duration_seconds(stats: Any) -> float | None:
    started = _parse_time(_safe_getattr(stats, "started_at"))
    completed = _parse_time(_safe_getattr(stats, "completed_at"))
    if started is None or completed is None:
        return None
    return round((completed - started).total_seconds(), 3)


def _extract_model_usage(stats: Any) -> dict[str, JsonValue]:
    usage = _safe_getattr(stats, "model_usage", {}) or {}
    normalized = _normalize(usage)
    assert isinstance(normalized, dict)
    return normalized


@lru_cache(maxsize=1)
def _load_models_dev_pricing() -> PricingData:
    payload, source = _load_models_dev_payload()
    return PricingData(model_costs=_flatten_models_dev_costs(payload), source=source)


def _load_models_dev_payload() -> tuple[dict[str, Any], str]:
    if MODELS_DEV_CACHE_PATH.exists():
        with MODELS_DEV_CACHE_PATH.open() as cache_file:
            payload = json.load(cache_file)
        if not isinstance(payload, dict):
            raise ValueError("cached models.dev API response is not a JSON object")
        return payload, f"cache:{MODELS_DEV_CACHE_PATH}"

    request = Request(
        MODELS_DEV_API_URL,
        headers={"User-Agent": "skill-evals-compare/0.1"},
    )
    with urlopen(request) as response:
        payload = json.load(response)

    if not isinstance(payload, dict):
        raise ValueError("models.dev API response is not a JSON object")

    MODELS_DEV_CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
    with MODELS_DEV_CACHE_PATH.open("w") as cache_file:
        json.dump(payload, cache_file, sort_keys=True)

    return payload, f"remote:{MODELS_DEV_API_URL}"


def _flatten_models_dev_costs(payload: dict[str, Any]) -> dict[str, dict[str, float]]:
    
    model_costs: dict[str, dict[str, float]] = {}
    for provider_id, provider_data in payload.items():
        if not isinstance(provider_data, dict):
            continue
        models = provider_data.get("models", {})
        if not isinstance(models, dict):
            continue
        for model_id, model_data in models.items():
            if not isinstance(model_data, dict):
                continue
            cost_data = model_data.get("cost", {})
            if not isinstance(cost_data, dict):
                continue
            normalized_cost: dict[str, float] = {}
            for key, value in cost_data.items():
                if isinstance(value, (int, float)):
                    normalized_cost[str(key)] = float(value)
            if not normalized_cost:
                continue
            qualified_model_id = str(model_data.get("id") or model_id)
            model_costs[qualified_model_id] = normalized_cost
            provider_qualified_model_id = f"{provider_id}/{qualified_model_id}"
            model_costs.setdefault(provider_qualified_model_id, normalized_cost)

    return model_costs


def _lookup_model_cost(
    model_costs: dict[str, dict[str, float]], model_id: str
) -> dict[str, float] | None:
    exact_match = model_costs.get(model_id)
    if exact_match is not None:
        return exact_match

    provider, separator, provider_model_id = model_id.partition("/")
    if not separator:
        return None

    aliased_provider = MODELS_DEV_PROVIDER_ALIASES.get(provider)
    if aliased_provider is None:
        return None

    return model_costs.get(f"{aliased_provider}/{provider_model_id}")


def _compute_usage_cost(model_usage: dict[str, JsonValue]) -> CostComputation:
    return _compute_usage_cost_with_model_costs(model_usage, None)


def _compute_usage_cost_with_model_costs(
    model_usage: dict[str, JsonValue], model_costs: dict[str, dict[str, float]] | None
) -> CostComputation:
    if not model_usage:
        return CostComputation(total_cost=None, warnings=["model usage is empty; cost omitted"])

    resolved_model_costs = model_costs
    if resolved_model_costs is None:
        try:
            resolved_model_costs = _load_models_dev_pricing().model_costs
        except (URLError, OSError, ValueError, json.JSONDecodeError) as exc:
            return CostComputation(
                total_cost=None,
                warnings=[f"unable to load pricing from {MODELS_DEV_API_URL}: {exc}"],
            )

    total_cost = 0.0
    matched_usage = False
    warnings: list[str] = []
    usage_fields = {
        "input_tokens": "input",
        "output_tokens": "output",
        "input_tokens_cache_read": "cache_read",
        "input_tokens_cache_write": "cache_write",
        "reasoning_tokens": "reasoning",
    }

    for model_id, raw_usage in sorted(model_usage.items()):
        if not isinstance(raw_usage, dict):
            warnings.append(f"usage for model {model_id!r} is not structured; cost omitted")
            continue

        cost = _lookup_model_cost(resolved_model_costs, model_id)
        if cost is None:
            warnings.append(f"no models.dev pricing found for model {model_id!r}; cost omitted")
            continue

        matched_usage = True
        for usage_field, cost_field in usage_fields.items():
            token_count = raw_usage.get(usage_field)
            if not isinstance(token_count, (int, float)):
                continue
            if token_count == 0:
                continue

            unit_cost = cost.get(cost_field)
            if unit_cost is None:
                warnings.append(
                    f"models.dev pricing for model {model_id!r} does not include {cost_field!r}; {usage_field} omitted"
                )
                continue

            total_cost += float(token_count) * unit_cost / 1_000_000

    if not matched_usage:
        return CostComputation(total_cost=None, warnings=warnings)

    return CostComputation(total_cost=round(total_cost, 6), warnings=warnings)


def _extract_scores(log: Any) -> dict[str, Any]:
    if log.results is None:
        return {}

    scores: dict[str, Any] = {}
    for index, score in enumerate(log.results.scores, start=1):
        score_key = _score_key(score, index, scores)
        entry: dict[str, Any] = {
            "key": score_key,
            "name": score.name,
            "scorer": score.scorer,
            "metrics": {
                metric.name: _normalize(metric.value) for metric in score.metrics.values()
            },
        }
        reducer = _safe_getattr(score, "reducer")
        if reducer is not None:
            entry["reducer"] = reducer
        scores[score_key] = entry
    return scores


def _score_key(score: Any, index: int, existing_scores: dict[str, Any]) -> str:
    base = str(
        _safe_getattr(score, "name")
        or _safe_getattr(score, "scorer")
        or f"score_{index}"
    )
    if base not in existing_scores:
        return base

    scorer = _safe_getattr(score, "scorer")
    if scorer:
        candidate = f"{base} [{scorer}]"
        if candidate not in existing_scores:
            return candidate

    suffix = 2
    while True:
        candidate = f"{base} #{suffix}"
        if candidate not in existing_scores:
            return candidate
        suffix += 1


def _extract_skill_enabled_sources(log: Any) -> dict[str, JsonValue]:
    eval_metadata = _safe_getattr(log.eval, "metadata", {}) or {}
    top_metadata = getattr(log, "metadata", {}) or {}
    task_args = _safe_getattr(log.eval, "task_args", {}) or {}
    return {
        "task_args": _normalize(task_args.get("skill_enabled")),
        "eval_metadata": _normalize(eval_metadata.get("skill_enabled")),
        "top_metadata": _normalize(top_metadata.get("skill_enabled")),
    }


def _build_coordinate_values(log: Any) -> dict[str, JsonValue]:
    dataset = _safe_getattr(log.eval, "dataset")
    task_identity = {
        "task": _safe_getattr(log.eval, "task"),
        "task_display_name": _safe_getattr(log.eval, "task_display_name"),
        "task_registry_name": _safe_getattr(log.eval, "task_registry_name"),
        "dataset_location": _safe_getattr(dataset, "location"),
        "sample_ids": _normalize(_safe_getattr(dataset, "sample_ids", [])),
    }
    return {
        "task": _normalize(task_identity),
        "model": _normalize(_safe_getattr(log.eval, "model")),
        "model_generate_config": _normalize(
            _safe_getattr(log.eval, "model_generate_config", {})
        ),
        "task_args": _normalize(_safe_getattr(log.eval, "task_args", {})),
        "skill_enabled": _normalize(_extract_skill_enabled_sources(log)),
        "config": _normalize(
            _safe_getattr(
                _safe_getattr(log.eval, "config"),
                "model_dump",
                lambda: _safe_getattr(log.eval, "config"),
            )()
        ),
        "metadata": _normalize(
            {
                "eval_metadata": _safe_getattr(log.eval, "metadata", {}),
                "top_metadata": getattr(log, "metadata", {}),
            }
        ),
    }


def _summarize_log(path: str, log: Any) -> dict[str, JsonValue]:
    stats = log.stats
    eval_config = _safe_getattr(log.eval, "config")
    return {
        "path": path,
        "status": log.status,
        "task": _safe_getattr(log.eval, "task"),
        "task_id": _safe_getattr(log.eval, "task_id"),
        "run_id": _safe_getattr(log.eval, "run_id"),
        "created": _safe_getattr(log.eval, "created"),
        "model": _safe_getattr(log.eval, "model"),
        "task_args": _normalize(_safe_getattr(log.eval, "task_args", {})),
        "eval_metadata": _normalize(_safe_getattr(log.eval, "metadata", {})),
        "metadata": _normalize(getattr(log, "metadata", {})),
        "model_generate_config": _normalize(
            _safe_getattr(log.eval, "model_generate_config", {})
        ),
        "config": _normalize(
            eval_config.model_dump() if hasattr(eval_config, "model_dump") else eval_config
        ),
        "started_at": _safe_getattr(stats, "started_at"),
        "completed_at": _safe_getattr(stats, "completed_at"),
        "duration_seconds": _compute_duration_seconds(stats),
        "model_usage": _extract_model_usage(stats),
        "scores": _extract_scores(log),
        "total_samples": _safe_getattr(log.results, "total_samples"),
        "completed_samples": _safe_getattr(log.results, "completed_samples"),
    }


def _headline_metrics(summary: dict[str, JsonValue]) -> dict[str, float | None]:
    scores = summary.get("scores", {})
    if not isinstance(scores, dict) or not scores:
        return {"score": None, "stderr": None}

    headline_score_key = summary.get("headline_score_key")
    if not isinstance(headline_score_key, str) or not headline_score_key:
        return {"score": None, "stderr": None}

    headline_score = scores.get(headline_score_key)
    if not isinstance(headline_score, dict):
        return {"score": None, "stderr": None}

    metrics = headline_score.get("metrics", {})
    if not isinstance(metrics, dict):
        return {"score": None, "stderr": None}

    score_value = metrics.get("accuracy")
    stderr_value = metrics.get("stderr")
    return {
        "score": float(score_value) if isinstance(score_value, (int, float)) else None,
        "stderr": float(stderr_value) if isinstance(stderr_value, (int, float)) else None,
    }


def _total_tokens(summary: dict[str, JsonValue]) -> int | None:
    usage = summary.get("model_usage", {})
    if not isinstance(usage, dict):
        return None
    total = 0
    found = False
    for provider_usage in usage.values():
        if not isinstance(provider_usage, dict):
            continue
        token_value = provider_usage.get("total_tokens")
        if isinstance(token_value, (int, float)):
            total += int(token_value)
            found = True
    return total if found else None


def _percent_delta(left: float | int | None, right: float | int | None) -> float | None:
    if left is None or right is None:
        return None
    baseline = float(left)
    if baseline == 0:
        return None
    return round(((float(right) - baseline) / baseline) * 100, 4)


def _resolve_headline_score_key(
    left_summary: dict[str, JsonValue],
    right_summary: dict[str, JsonValue],
    preferred_key: str | None,
) -> tuple[str | None, list[str]]:
    warnings: list[str] = []
    left_scores = left_summary.get("scores", {})
    right_scores = right_summary.get("scores", {})
    left_keys = sorted(left_scores) if isinstance(left_scores, dict) else []
    right_keys = sorted(right_scores) if isinstance(right_scores, dict) else []
    shared_keys = sorted(set(left_keys) & set(right_keys))

    if preferred_key:
        if preferred_key in shared_keys:
            return preferred_key, warnings
        warnings.append(
            f"headline score key {preferred_key!r} not present in both runs; headline score metrics omitted"
        )
        return None, warnings

    if len(shared_keys) == 1:
        return shared_keys[0], warnings

    if not shared_keys:
        if left_keys or right_keys:
            warnings.append(
                "runs do not share a common score key; headline score metrics omitted"
            )
        return None, warnings

    warnings.append(
        "multiple score keys are available; use --headline-score to choose one explicitly"
    )
    return None, warnings


def _detect_coordinate_diffs(
    left_summary: dict[str, JsonValue], right_summary: dict[str, JsonValue]
) -> list[CoordinateDiff]:
    left_coords = left_summary["coordinates"]
    right_coords = right_summary["coordinates"]
    assert isinstance(left_coords, dict)
    assert isinstance(right_coords, dict)

    diffs: list[CoordinateDiff] = []
    for coordinate in sorted(set(left_coords) | set(right_coords)):
        left_value = left_coords.get(coordinate)
        right_value = right_coords.get(coordinate)
        reduced_diff = _diff_value(left_value, right_value)
        if reduced_diff is not None:
            diff_left, diff_right = reduced_diff
            diffs.append(
                CoordinateDiff(coordinate=coordinate, left=diff_left, right=diff_right)
            )
    return diffs


def _skill_enabled_warnings(summary: dict[str, JsonValue], side: str) -> list[str]:
    coordinates = summary["coordinates"]
    assert isinstance(coordinates, dict)
    skill_enabled = coordinates.get("skill_enabled", {})
    if not isinstance(skill_enabled, dict):
        return []
    values = {name: value for name, value in skill_enabled.items() if value is not None}
    if len(set(json.dumps(_normalize(value), sort_keys=True) for value in values.values())) <= 1:
        return []
    rendered = ", ".join(f"{key}={value!r}" for key, value in values.items())
    return [f"{side}: inconsistent skill_enabled values across sources ({rendered})"]


def _serialize_score_values(scores: Any) -> dict[str, JsonValue]:
    if not isinstance(scores, dict):
        return {}
    serialized: dict[str, JsonValue] = {}
    for name, score in sorted(scores.items()):
        value = _safe_getattr(score, "value")
        serialized[name] = _normalize(value)
    return serialized


def _serialize_sample_usage(model_usage: Any) -> dict[str, JsonValue]:
    if not isinstance(model_usage, dict):
        return {}
    return _normalize(model_usage)


def _sum_usage_total_tokens(model_usage: dict[str, JsonValue]) -> int | None:
    total = 0
    found = False
    for provider_usage in model_usage.values():
        if not isinstance(provider_usage, dict):
            continue
        token_value = provider_usage.get("total_tokens")
        if isinstance(token_value, (int, float)):
            total += int(token_value)
            found = True
    return total if found else None


def _mean(values: list[float]) -> float | None:
    if not values:
        return None
    return round(sum(values) / len(values), 6)


def _deduplicate_warnings(warnings: list[str]) -> list[str]:
    return list(dict.fromkeys(warnings))


def _score_list_mean(score_values: dict[str, list[float]]) -> dict[str, JsonValue]:
    return {name: _mean(values) for name, values in score_values.items()}


def _score_list_min(score_values: dict[str, list[float]]) -> dict[str, JsonValue]:
    return {name: min(values) if values else None for name, values in score_values.items()}


def _score_list_max(score_values: dict[str, list[float]]) -> dict[str, JsonValue]:
    return {name: max(values) if values else None for name, values in score_values.items()}


def _aggregate_sample_entry(
    samples: list[Any], model_costs: dict[str, dict[str, float]] | None
) -> tuple[dict[str, JsonValue], list[str]]:
    score_values: dict[str, list[float]] = {}
    total_times: list[float] = []
    working_times: list[float] = []
    total_tokens_per_epoch: list[float] = []
    total_costs_per_epoch: list[float] = []
    error_count = 0
    limit_count = 0
    warnings: list[str] = []

    for sample in samples:
        serialized_scores = _serialize_score_values(_safe_getattr(sample, "scores", {}))
        for score_name, score_value in serialized_scores.items():
            if isinstance(score_value, (int, float)):
                score_values.setdefault(score_name, []).append(float(score_value))

        total_time = _safe_getattr(sample, "total_time")
        if isinstance(total_time, (int, float)):
            total_times.append(float(total_time))

        working_time = _safe_getattr(sample, "working_time")
        if isinstance(working_time, (int, float)):
            working_times.append(float(working_time))

        usage = _serialize_sample_usage(_safe_getattr(sample, "model_usage", {}))
        usage_total = _sum_usage_total_tokens(usage)
        if usage_total is not None:
            total_tokens_per_epoch.append(float(usage_total))
        usage_cost = _compute_usage_cost_with_model_costs(usage, model_costs)
        if usage_cost.total_cost is not None:
            total_costs_per_epoch.append(float(usage_cost.total_cost))
        warnings.extend(usage_cost.warnings)

        if _safe_getattr(sample, "error") is not None:
            error_count += 1
        if _safe_getattr(sample, "limit") is not None:
            limit_count += 1

    success_count = 0
    failure_count = 0
    for values in score_values.values():
        success_count += sum(1 for value in values if value >= 1)
        failure_count += sum(1 for value in values if value <= 0)

    total_tokens = int(sum(total_tokens_per_epoch)) if total_tokens_per_epoch else None
    avg_tokens_per_epoch = _mean(total_tokens_per_epoch)
    total_cost = round(sum(total_costs_per_epoch), 6) if total_costs_per_epoch else None
    avg_cost_per_epoch = _mean(total_costs_per_epoch)

    return (
        {
            "epochs": len(samples),
            "score_values": {
                name: [_round_number(value) for value in values]
                for name, values in sorted(score_values.items())
            },
            "mean_score": _score_list_mean(score_values),
            "min_score": _score_list_min(score_values),
            "max_score": _score_list_max(score_values),
            "success_count": success_count,
            "failure_count": failure_count,
            "avg_total_time": _mean(total_times),
            "avg_working_time": _mean(working_times),
            "total_tokens": total_tokens,
            "avg_tokens_per_epoch": avg_tokens_per_epoch,
            "total_cost": total_cost,
            "avg_cost_per_epoch": avg_cost_per_epoch,
            "has_error": error_count > 0,
            "error_count": error_count,
            "has_limit": limit_count > 0,
            "limit_count": limit_count,
        },
        _deduplicate_warnings(warnings),
    )


def _group_samples_by_entry(path: str) -> dict[str, list[Any]]:
    grouped: dict[str, list[Any]] = {}
    for sample in read_eval_log_sample_summaries(path):
        grouped.setdefault(str(sample.id), []).append(sample)
    for samples in grouped.values():
        samples.sort(key=lambda sample: int(sample.epoch))
    return grouped


def _compare_sample_entries(
    left_path: str,
    right_path: str,
    model_costs: dict[str, dict[str, float]] | None,
) -> tuple[dict[str, Any], list[str]]:
    left_entries = _group_samples_by_entry(left_path)
    right_entries = _group_samples_by_entry(right_path)

    all_sample_ids = sorted(set(left_entries) | set(right_entries))
    comparisons: list[dict[str, Any]] = []
    differing_entries = 0
    warnings: list[str] = []

    for sample_id in all_sample_ids:
        left_aggregate, left_warnings = _aggregate_sample_entry(
            left_entries.get(sample_id, []), model_costs
        )
        right_aggregate, right_warnings = _aggregate_sample_entry(
            right_entries.get(sample_id, []), model_costs
        )
        warnings.extend(f"sample left: {warning}" for warning in left_warnings)
        warnings.extend(f"sample right: {warning}" for warning in right_warnings)
        different_fields = [
            field
            for field in left_aggregate.keys()
            if not _deep_equal(left_aggregate[field], right_aggregate[field])
        ]
        if different_fields:
            differing_entries += 1
        comparisons.append(
            {
                "sample_id": sample_id,
                "different_fields": different_fields,
                "left": left_aggregate,
                "right": right_aggregate,
            }
        )

    return (
        {
            "total_entries": len(all_sample_ids),
            "entries_with_differences": differing_entries,
            "comparisons": comparisons,
        },
        _deduplicate_warnings(warnings),
    )


def build_report(
    left_path: str,
    right_path: str,
    expect_diff: set[str],
    include_samples: bool,
    headline_score: str | None = None,
) -> dict[str, Any]:
    left_log = read_eval_log(left_path, header_only=True)
    right_log = read_eval_log(right_path, header_only=True)

    left_summary = _summarize_log(left_path, left_log)
    right_summary = _summarize_log(right_path, right_log)
    left_summary["coordinates"] = _build_coordinate_values(left_log)
    right_summary["coordinates"] = _build_coordinate_values(right_log)

    headline_score_key, headline_warnings = _resolve_headline_score_key(
        left_summary, right_summary, headline_score
    )
    left_summary["headline_score_key"] = headline_score_key
    right_summary["headline_score_key"] = headline_score_key

    coordinate_diffs = _detect_coordinate_diffs(left_summary, right_summary)
    allowed_diff = _expand_expected_diff(expect_diff)
    unexpected = [
        diff.coordinate for diff in coordinate_diffs if diff.coordinate not in allowed_diff
    ]

    left_headline = _headline_metrics(left_summary)
    right_headline = _headline_metrics(right_summary)
    left_tokens = _total_tokens(left_summary)
    right_tokens = _total_tokens(right_summary)
    pricing_warnings: list[str] = []
    model_costs: dict[str, dict[str, float]] | None = None
    try:
        pricing = _load_models_dev_pricing()
        model_costs = pricing.model_costs
    except (URLError, OSError, ValueError, json.JSONDecodeError) as exc:
        pricing_warnings.append(
            f"unable to load pricing from {MODELS_DEV_API_URL}: {exc}"
        )
    left_cost = _compute_usage_cost_with_model_costs(left_summary["model_usage"], model_costs)
    right_cost = _compute_usage_cost_with_model_costs(right_summary["model_usage"], model_costs)
    sample_entry_differences: dict[str, Any] | None = None
    sample_warnings: list[str] = []
    if include_samples:
        sample_entry_differences, sample_warnings = _compare_sample_entries(
            left_path, right_path, model_costs
        )

    warnings = []
    warnings.extend(headline_warnings)
    warnings.extend(_skill_enabled_warnings(left_summary, "left"))
    warnings.extend(_skill_enabled_warnings(right_summary, "right"))
    warnings.extend(pricing_warnings)
    warnings.extend(f"left: {warning}" for warning in left_cost.warnings)
    warnings.extend(f"right: {warning}" for warning in right_cost.warnings)
    warnings.extend(sample_warnings)
    if left_summary["status"] != "success":
        warnings.append(f"left: status is {left_summary['status']!r}, results may be partial")
    if right_summary["status"] != "success":
        warnings.append(f"right: status is {right_summary['status']!r}, results may be partial")
    if unexpected:
        warnings.append(
            "unexpected coordinate differences found: " + ", ".join(sorted(unexpected))
        )

    return {
        "left": left_summary,
        "right": right_summary,
        "expected_diff": sorted(expect_diff),
        "headline_score_key": headline_score_key,
        "allowed_diff_coordinates": sorted(allowed_diff),
        "actual_differences": [
            {"coordinate": diff.coordinate, "left": diff.left, "right": diff.right}
            for diff in coordinate_diffs
        ],
        "unexpected_differences": sorted(unexpected),
        "summary": {
            "score": {
                "left": left_headline["score"],
                "right": right_headline["score"],
                "delta": (right_headline["score"] - left_headline["score"])
                if left_headline["score"] is not None
                and right_headline["score"] is not None
                else None,
                "percent_delta": _percent_delta(
                    left_headline["score"], right_headline["score"]
                ),
            },
            "stderr": {
                "left": left_headline["stderr"],
                "right": right_headline["stderr"],
                "delta": (right_headline["stderr"] - left_headline["stderr"])
                if left_headline["stderr"] is not None
                and right_headline["stderr"] is not None
                else None,
                "percent_delta": _percent_delta(
                    left_headline["stderr"], right_headline["stderr"]
                ),
            },
            "duration_seconds": {
                "left": left_summary["duration_seconds"],
                "right": right_summary["duration_seconds"],
                "delta": (
                    right_summary["duration_seconds"] - left_summary["duration_seconds"]
                    if isinstance(left_summary["duration_seconds"], (int, float))
                    and isinstance(right_summary["duration_seconds"], (int, float))
                    else None
                ),
                "percent_delta": _percent_delta(
                    left_summary["duration_seconds"], right_summary["duration_seconds"]
                ),
            },
            "total_tokens": {
                "left": left_tokens,
                "right": right_tokens,
                "delta": (right_tokens - left_tokens)
                if left_tokens is not None and right_tokens is not None
                else None,
                "percent_delta": _percent_delta(left_tokens, right_tokens),
            },
            "total_cost": {
                "left": left_cost.total_cost,
                "right": right_cost.total_cost,
                "delta": (right_cost.total_cost - left_cost.total_cost)
                if left_cost.total_cost is not None and right_cost.total_cost is not None
                else None,
                "percent_delta": _percent_delta(
                    left_cost.total_cost, right_cost.total_cost
                ),
            },
        },
        "sample_entry_differences": sample_entry_differences,
        "warnings": _deduplicate_warnings(warnings),
    }
