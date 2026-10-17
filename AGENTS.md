# AGENTS.md — mulighetsrommet

## Build & Test Commands

### Backend

In repository root:
```bash
./gradlew test          # Run tests
./gradlew build         # Build
./gradlew ktlintFormat  # Lint and format code
```

### Frontend:

In repository root:
```bash
turbo run test --filter=<frontend-name>         # Run tests
turbo run typecheck  --filter=<frontend-name>   # Geneates typescript types from OpenApi specs and performs a type check
turbo run lint:fix --filter=<frontend-name>     # Fixes lints and formats
```

Where <frontend-name> is the name in `package.json` for apps in `frontend/`.

## Project Structure

```text
common/
docs/
frontend/
gradle/
iac/
mulighetsrommet-api/
mulighetsrommet-arena-adapter/
mulighetsrommet-pdfgenrs/
mulighetsrommet-tiltakshistorikk/
mulighetsrommet-tiltaksokonomi/
onboarding/
scripts/
```

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

## Git Workflow

In this repository, developers create short-lived feature branches off main, open a Pull Request (PR) for code review, and merge back into main.

## Boundaries

### ✅ Always

- Run linting and tests after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries
