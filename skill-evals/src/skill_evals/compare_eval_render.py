from __future__ import annotations

import json
from datetime import datetime
from typing import Any

from .compare_eval_report import JsonValue


def _headline_score_from_scores(
    scores: dict[str, JsonValue], headline_score_key: str | None
) -> float | None:
    if not isinstance(scores, dict) or not scores:
        return None
    if not headline_score_key:
        return None
    score_value = scores.get(headline_score_key)
    if isinstance(score_value, (int, float)):
        return float(score_value)
    return None


def _format_float(value: float | None, digits: int = 4) -> str:
    if value is None:
        return "n/a"
    return f"{value:.{digits}f}"


def _format_currency(value: float | None, digits: int = 4) -> str:
    if value is None:
        return "n/a"
    return f"${value:.{digits}f}"


def _format_delta(
    left: float | int | None, right: float | int | None, digits: int = 4
) -> str:
    if left is None or right is None:
        return "n/a"
    delta = right - left
    if isinstance(left, int) and isinstance(right, int):
        return f"{delta:+d}"
    return f"{delta:+.{digits}f}"


def _percent_delta(left: float | int | None, right: float | int | None) -> float | None:
    if left is None or right is None:
        return None
    baseline = float(left)
    if baseline == 0:
        return None
    return round(((float(right) - baseline) / baseline) * 100, 4)


def _format_percent_delta(
    left: float | int | None, right: float | int | None, digits: int = 1
) -> str:
    percent = _percent_delta(left, right)
    if percent is None:
        return "n/a"
    return f"{percent:+.{digits}f}%"


def _sample_entry_metric_row(sample: dict[str, Any]) -> list[str]:
    headline_score_key = sample.get("headline_score_key")
    left = sample["left"]
    right = sample["right"]
    left_score = _headline_score_from_scores(left.get("mean_score", {}), headline_score_key)
    right_score = _headline_score_from_scores(
        right.get("mean_score", {}), headline_score_key
    )
    left_avg_tokens = left.get("avg_tokens_per_epoch")
    right_avg_tokens = right.get("avg_tokens_per_epoch")
    left_avg_cost = left.get("avg_cost_per_epoch")
    right_avg_cost = right.get("avg_cost_per_epoch")
    left_avg_total_time = left.get("avg_total_time")
    right_avg_total_time = right.get("avg_total_time")
    return [
        sample["sample_id"],
        _format_float(left_score),
        _format_float(right_score),
        _format_delta(left_score, right_score),
        _format_percent_delta(left_score, right_score),
        _format_float(left_avg_tokens, 1),
        _format_float(right_avg_tokens, 1),
        _format_delta(left_avg_tokens, right_avg_tokens, 1),
        _format_percent_delta(left_avg_tokens, right_avg_tokens),
        _format_currency(left_avg_cost),
        _format_currency(right_avg_cost),
        _format_delta(left_avg_cost, right_avg_cost),
        _format_percent_delta(left_avg_cost, right_avg_cost),
        _format_float(left_avg_total_time, 3),
        _format_float(right_avg_total_time, 3),
        _format_delta(left_avg_total_time, right_avg_total_time, 3),
        _format_percent_delta(left_avg_total_time, right_avg_total_time),
    ]


def _json_default(value: Any) -> Any:
    if isinstance(value, datetime):
        return value.isoformat()
    raise TypeError(f"Object of type {type(value).__name__} is not JSON serializable")


def render_json(report: dict[str, Any]) -> str:
    return json.dumps(report, indent=2, sort_keys=True, default=_json_default)


def _render_table(headers: list[str], rows: list[list[str]]) -> str:
    widths = [len(header) for header in headers]
    for row in rows:
        for index, value in enumerate(row):
            widths[index] = max(widths[index], len(value))

    def render_row(row: list[str]) -> str:
        return "  ".join(value.ljust(widths[index]) for index, value in enumerate(row))

    lines = [render_row(headers), render_row(["-" * width for width in widths])]
    lines.extend(render_row(row) for row in rows)
    return "\n".join(lines)


def render_text(report: dict[str, Any]) -> str:
    left = report["left"]
    right = report["right"]
    summary = report["summary"]
    headline_score_key = report.get("headline_score_key")
    lines = ["Eval Comparison", ""]
    lines.append("Runs")
    lines.append(
        _render_table(
            ["Side", "Task", "Model", "Log"],
            [
                ["left", str(left["task"]), str(left["model"]), str(left["path"])],
                ["right", str(right["task"]), str(right["model"]), str(right["path"])],
            ],
        )
    )
    lines.append("")
    lines.append("Expected differences: " + (", ".join(report["expected_diff"]) or "none"))
    actual = [diff["coordinate"] for diff in report["actual_differences"]]
    lines.append("Actual differences: " + (", ".join(actual) or "none"))
    lines.append("")
    lines.append("Outcome summary")
    if isinstance(headline_score_key, str) and headline_score_key:
        lines.append(f"Selected scorer key: {headline_score_key}")
    lines.append(
        _render_table(
            ["Metric", "left", "right", "delta", "% delta"],
            [
                [
                    "headline score",
                    _format_float(summary["score"]["left"]),
                    _format_float(summary["score"]["right"]),
                    _format_delta(summary["score"]["left"], summary["score"]["right"]),
                    _format_percent_delta(
                        summary["score"]["left"], summary["score"]["right"]
                    ),
                ],
                [
                    "stderr",
                    _format_float(summary["stderr"]["left"]),
                    _format_float(summary["stderr"]["right"]),
                    _format_delta(
                        summary["stderr"]["left"], summary["stderr"]["right"]
                    ),
                    _format_percent_delta(
                        summary["stderr"]["left"], summary["stderr"]["right"]
                    ),
                ],
                [
                    "duration_seconds",
                    _format_float(summary["duration_seconds"]["left"], 3),
                    _format_float(summary["duration_seconds"]["right"], 3),
                    _format_delta(
                        summary["duration_seconds"]["left"],
                        summary["duration_seconds"]["right"],
                        3,
                    ),
                    _format_percent_delta(
                        summary["duration_seconds"]["left"],
                        summary["duration_seconds"]["right"],
                    ),
                ],
                [
                    "total_tokens",
                    str(
                        summary["total_tokens"]["left"]
                        if summary["total_tokens"]["left"] is not None
                        else "n/a"
                    ),
                    str(
                        summary["total_tokens"]["right"]
                        if summary["total_tokens"]["right"] is not None
                        else "n/a"
                    ),
                    _format_delta(
                        summary["total_tokens"]["left"], summary["total_tokens"]["right"]
                    ),
                    _format_percent_delta(
                        summary["total_tokens"]["left"], summary["total_tokens"]["right"]
                    ),
                ],
                [
                    "total_cost_usd",
                    _format_currency(summary["total_cost"]["left"]),
                    _format_currency(summary["total_cost"]["right"]),
                    _format_delta(summary["total_cost"]["left"], summary["total_cost"]["right"]),
                    _format_percent_delta(
                        summary["total_cost"]["left"], summary["total_cost"]["right"]
                    ),
                ],
            ],
        )
    )
    lines.append("")
    lines.append("Coordinate differences")
    if report["actual_differences"]:
        lines.append(
            _render_table(
                ["Coordinate", "left", "right"],
                [
                    [
                        diff["coordinate"],
                        json.dumps(diff["left"], sort_keys=True),
                        json.dumps(diff["right"], sort_keys=True),
                    ]
                    for diff in report["actual_differences"]
                ],
            )
        )
    else:
        lines.append("none")

    sample_differences = report["sample_entry_differences"]
    if sample_differences is not None:
        lines.append("")
        lines.append("Sample differences")
        lines.append(
            f"entries with differences: {sample_differences['entries_with_differences']} / {sample_differences['total_entries']}"
        )
        differing_rows = [
            _sample_entry_metric_row(
                {**sample, "headline_score_key": report.get("headline_score_key")}
            )
            for sample in sample_differences["comparisons"]
            if sample["different_fields"]
        ]
        if differing_rows:
            lines.append(
                _render_table(
                    [
                        "sample_id",
                        "left_score",
                        "right_score",
                        "score_delta",
                        "score_%_delta",
                        "left_avg_tokens",
                        "right_avg_tokens",
                        "tokens_delta",
                        "tokens_%_delta",
                        "left_avg_cost",
                        "right_avg_cost",
                        "cost_delta",
                        "cost_%_delta",
                        "left_avg_total_time",
                        "right_avg_total_time",
                        "time_delta",
                        "time_%_delta",
                    ],
                    differing_rows,
                )
            )
        else:
            lines.append("none")

    lines.append("")
    lines.append("Warnings")
    if report["warnings"]:
        lines.extend(f"- {warning}" for warning in report["warnings"])
    else:
        lines.append("- none")
    return "\n".join(lines)


def _markdown_escape(value: Any) -> str:
    text = json.dumps(value, sort_keys=True) if isinstance(value, (dict, list)) else str(value)
    return text.replace("|", "\\|")


def render_markdown(report: dict[str, Any]) -> str:
    left = report["left"]
    right = report["right"]
    summary = report["summary"]
    headline_score_key = report.get("headline_score_key")
    lines = [
        "# Eval Comparison",
        "",
        "## Runs",
        "",
        "| Side | Task | Model | Log |",
        "|---|---|---|---|",
    ]
    lines.append(
        f"| left | `{_markdown_escape(left['task'])}` | `{_markdown_escape(left['model'])}` | `{_markdown_escape(left['path'])}` |"
    )
    lines.append(
        f"| right | `{_markdown_escape(right['task'])}` | `{_markdown_escape(right['model'])}` | `{_markdown_escape(right['path'])}` |"
    )
    lines.extend(["", "## Expected Differences", ""])
    if report["expected_diff"]:
        lines.extend(f"- `{coordinate}`" for coordinate in report["expected_diff"])
    else:
        lines.append("- None")

    lines.extend(
        [
            "",
            "## Outcome Summary",
            "",
        ]
    )
    if isinstance(headline_score_key, str) and headline_score_key:
        lines.extend([f"Selected scorer key: `{headline_score_key}`", ""])
    lines.extend(
        [
            "| Metric | left | right | delta | % delta |",
            "|---|---:|---:|---:|---:|",
        ]
    )
    lines.append(
        f"| Headline score | {_format_float(summary['score']['left'])} | {_format_float(summary['score']['right'])} | {_format_delta(summary['score']['left'], summary['score']['right'])} | {_format_percent_delta(summary['score']['left'], summary['score']['right'])} |"
    )
    lines.append(
        f"| Stderr | {_format_float(summary['stderr']['left'])} | {_format_float(summary['stderr']['right'])} | {_format_delta(summary['stderr']['left'], summary['stderr']['right'])} | {_format_percent_delta(summary['stderr']['left'], summary['stderr']['right'])} |"
    )
    lines.append(
        f"| Duration (s) | {_format_float(summary['duration_seconds']['left'], 3)} | {_format_float(summary['duration_seconds']['right'], 3)} | {_format_delta(summary['duration_seconds']['left'], summary['duration_seconds']['right'], 3)} | {_format_percent_delta(summary['duration_seconds']['left'], summary['duration_seconds']['right'])} |"
    )
    left_tokens = summary["total_tokens"]["left"]
    right_tokens = summary["total_tokens"]["right"]
    lines.append(
        f"| Total tokens | {left_tokens if left_tokens is not None else 'n/a'} | {right_tokens if right_tokens is not None else 'n/a'} | {_format_delta(left_tokens, right_tokens)} | {_format_percent_delta(left_tokens, right_tokens)} |"
    )
    lines.append(
        f"| Total cost (USD) | {_format_currency(summary['total_cost']['left'])} | {_format_currency(summary['total_cost']['right'])} | {_format_delta(summary['total_cost']['left'], summary['total_cost']['right'])} | {_format_percent_delta(summary['total_cost']['left'], summary['total_cost']['right'])} |"
    )

    lines.extend(["", "## Coordinate Differences", ""])
    if report["actual_differences"]:
        lines.append("| Coordinate | left | right |")
        lines.append("|---|---|---|")
        for diff in report["actual_differences"]:
            lines.append(
                f"| `{_markdown_escape(diff['coordinate'])}` | `{_markdown_escape(diff['left'])}` | `{_markdown_escape(diff['right'])}` |"
            )
    else:
        lines.append("None")

    sample_differences = report["sample_entry_differences"]
    if sample_differences is not None:
        lines.extend(["", "## Sample Differences", ""])
        lines.append(
            f"Entries with differences: `{sample_differences['entries_with_differences']} / {sample_differences['total_entries']}`"
        )
        differing = [
            sample
            for sample in sample_differences["comparisons"]
            if sample["different_fields"]
        ]
        if differing:
            lines.extend(
                [
                    "",
                    "| Sample ID | Left score | Right score | Score delta | Score % delta | Left avg tokens | Right avg tokens | Tokens delta | Tokens % delta | Left avg cost | Right avg cost | Cost delta | Cost % delta | Left avg total time | Right avg total time | Time delta | Time % delta |",
                    "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
                ]
            )
            for sample in differing:
                row = _sample_entry_metric_row(
                    {**sample, "headline_score_key": report.get("headline_score_key")}
                )
                lines.append(
                    f"| `{_markdown_escape(row[0])}` | {row[1]} | {row[2]} | {row[3]} | {row[4]} | {row[5]} | {row[6]} | {row[7]} | {row[8]} | {row[9]} | {row[10]} | {row[11]} | {row[12]} | {row[13]} | {row[14]} | {row[15]} | {row[16]} |"
                )

    lines.extend(["", "## Warnings", ""])
    if report["warnings"]:
        lines.extend(f"- {warning}" for warning in report["warnings"])
    else:
        lines.append("- None")
    return "\n".join(lines)
