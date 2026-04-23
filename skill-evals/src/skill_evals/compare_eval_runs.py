import argparse
import sys
from pathlib import Path

from skill_evals.compare_eval_render import render_json, render_markdown, render_text
from skill_evals.compare_eval_report import build_report


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare two Inspect eval runs by score, time, usage, and configuration."
    )
    parser.add_argument("left_log", help="Path or file:// URI for the left eval log")
    parser.add_argument("right_log", help="Path or file:// URI for the right eval log")
    parser.add_argument(
        "--expect-diff",
        default="",
        help=(
            "Comma-separated logical coordinates allowed to differ, e.g. "
            "skill_enabled,model"
        ),
    )
    parser.add_argument(
        "--format",
        choices=("text", "json", "markdown"),
        default="text",
        help="Output format",
    )
    parser.add_argument(
        "--samples",
        action="store_true",
        help="Include per-sample and per-epoch comparison details",
    )
    parser.add_argument(
        "--headline-score",
        default="",
        help=(
            "Score key to use for headline score reporting when runs contain multiple "
            "scores"
        ),
    )
    parser.add_argument(
        "--fail-on-unexpected-diff",
        action="store_true",
        help="Exit with status 1 when differences outside --expect-diff are found",
    )
    return parser.parse_args()


def _normalize_log_path(value: str) -> str:
    if value.startswith("file://"):
        return value
    return str(Path(value).expanduser().resolve())


def main() -> int:
    args = _parse_args()
    left_path = _normalize_log_path(args.left_log)
    right_path = _normalize_log_path(args.right_log)
    expected_diff = {item.strip() for item in args.expect_diff.split(",") if item.strip()}
    headline_score = args.headline_score.strip() or None
    report = build_report(
        left_path,
        right_path,
        expected_diff,
        args.samples,
        headline_score=headline_score,
    )

    if args.format == "json":
        output = render_json(report)
    elif args.format == "markdown":
        output = render_markdown(report)
    else:
        output = render_text(report)

    print(output)

    if args.fail_on_unexpected_diff and report["unexpected_differences"]:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
