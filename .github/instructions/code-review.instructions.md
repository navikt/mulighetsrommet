---
applyTo: "**"
excludeAgent: "cloud-agent"
---

# Code review

## Finding criteria

- Prioritize bugs, security vulnerabilities, data corruption, and breaking changes introduced or made worse by the PR. Use surrounding code, callers, and tests to check whether an apparent issue is handled elsewhere.
- Ground each finding in the code. Explain the triggering condition and concrete consequence. Do not present an unverified assumption as a confirmed defect.
- Do not report style preferences, speculative problems, or unrelated refactoring suggestions. Avoid comments that only repeat formatter, compiler, or lint diagnostics.
- Report each root cause once, at the most relevant location. No findings is a valid result; do not aim for a fixed number of comments.
- Flag unrelated edits only when they clearly exceed the stated PR scope or create a concrete review or regression risk. Allow necessary follow-up changes and regenerated files; diff size alone is not a finding.

## Security and privacy

- For changed routes, verify authentication and authorization against the existing route group's access model. Nais accessPolicy does not replace API authorization.
- For access to organizer resources, verify that the existing Altinn check covers the organization number belonging to the resource being read or changed. A valid TokenX identity alone does not grant access.
- Flag exposed secrets, tokens, or credentials, and personal data added to logs, error responses, or tracing. Distinguish real credentials from non-sensitive example values; do not reproduce sensitive values in review comments.
- Check that untrusted SQL values use bound parameters rather than string concatenation. For dynamic identifiers or sort expressions, verify an explicit allowlist rather than assuming value parameters can bind SQL syntax.
- For changed GitHub Actions workflows, check full commit-SHA pinning with a version comment, least-privilege permissions, and unsafe use of untrusted PR content in shell commands or privileged jobs.

## Data, contracts, and tests

- When changing database writes and related events in mulighetsrommet-api, verify that both remain in the same transaction through the existing QueryContext.outbox pattern. Check that required transaction boundaries and advisory locks are preserved.
- When changing jobs, message handling, or external side effects, check whether retries, redelivery, or parallel execution can duplicate effects or lose updates. Identify the concrete execution path rather than requesting retries or locks everywhere.
- For API, event, or serialization changes, check compatibility with existing consumers. For Flyway migrations, check existing data and whether old and new application versions can coexist during rollout; flag edits to already-applied migrations.
- For API changes, check that OpenAPI specifications and affected clients follow the existing generation flow. Do not request manual fixes to generated files instead of fixing their source.
- Identify specific changed behavior or failure scenarios missing test coverage, not generic requests for more tests. For bugfixes, check for a regression test that fails before the fix. Do not claim tests passed unless execution evidence is available.
- Flag changed error handling that silently discards failures, reports success after failure, or removes the information needed to diagnose a failed operation. Do not request logging of sensitive payloads.

## Frontend and language

- For changed user flows, check loading, empty, error, and success states, and whether TanStack Query cache updates or invalidation keep affected views consistent after mutations.
- Flag concrete accessibility regressions such as inaccessible interactive elements, missing form labels, lost keyboard focus, or errors users cannot perceive. Use Aksel patterns and spacing supported by the installed version and the app's Tailwind configuration; do not reject all p-* or m-* utilities.
- For Norwegian user-facing text, check bokmål, understandable error messages, and unnecessary anglicisms. Do not require all Markdown documentation or technical identifiers to be Norwegian.
