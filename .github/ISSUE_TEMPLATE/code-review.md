---
name: "Comprehensive code review — best practices & maintainability"
about: "Request a full repository code review focused on best practices, simplification, and maintainability."
title: "Full repository code review — best practices, simplification, and maintainability"
labels: ["code-review"]
assignees: []
---

## Context
- Goal: Improve code quality, reduce complexity, and increase maintainability across the repository.
- Scope: Full repository. Prioritize core modules (API surface, business logic, data access, security-sensitive code), CI, tests and docs.
- Target branch: `develop` (replace if different)
- How to run tests: `# insert commands here`

## What I need from reviewers
1. High-level summary (1–3 short paragraphs)
   - Overall health, biggest risks, most valuable refactors.

2. Detailed findings (one entry per issue)
   - File(s) and exact lines (or link).
   - Category: bug / security / style / performance / readability / maintainability / test coverage / CI / docs.
   - Problem: why it's an issue (impact + examples).
   - Proposed fix: concrete change or patch.
   - Suggested tests or verification steps.
   - Priority: P0 (must-fix), P1, P2.
   - Effort estimate: small / medium / large.

3. Simplifications & alternatives
   - For each simplification include:
     - Before (snippet/link)
     - After (replacement snippet)
     - Rationale: why it's clearer/safer/faster.

4. Maintainability recommendations
   - Architecture layering, modularization, testability, CI/linting, dependency pinning, docs.

## Deliverables & format
- Post a high-level summary in this issue.
- For concrete fixes either:
  - Open a focused PR with the fix that references this issue, or
  - Provide a pasteable patch (diff) and tests in a comment.
- Prefer small, focused PRs for P0/P1 fixes and a single larger PR for major refactors with migration notes.

## Checklist (please mark)
- [ ] Code style & linters
- [ ] Security
- [ ] Correctness (edge cases)
- [ ] Complexity (cyclomatic hotspots)
- [ ] Tests (coverage, flakiness)
- [ ] Performance
- [ ] Docs
- [ ] CI
- [ ] Dependency risks

## Example entry (copy/paste this format)
- Files: `path/to/file.py#L120-L145`
- Category: readability/performance
- Problem: This nested conditional duplicates validation logic and is hard to follow.
- Proposed fix: Replace with guard clause + helper:
  ```py
  # before
  if condA:
      if condB:
          do_x()
      else:
          handle_error()
  ```
  ```py
  # after
  def valid_for_x(obj):
      return condA and condB

  if not valid_for_x(obj):
      handle_error()
  do_x()
  ```
- Tests: Add unit tests for `valid_for_x` and the error path.
- Priority: P1
- Effort: small
- Suggested PR: link/to/pr or paste diff here

## Logistics & notes
- Run tests with: (paste commands)
- Please include benchmarks or test results for performance suggestions.
- If you open a PR, include a short description and link back to this issue.
- Replace placeholders above (`develop`, test commands) with project-specific values.
