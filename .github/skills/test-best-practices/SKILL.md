---
name: test-best-practices
description: Apply repository test best practices when creating, editing, or reviewing tests. Use independent explicit expectations, deterministic fixtures, and regression coverage at the failing boundary.
---

# Test best practices

Use these rules whenever creating, editing, or reviewing tests in this repository.

## Expectations must be independent

Expected values must state the intended behavior explicitly. Do not repeat production calculations,
call the implementation under test, or use equivalent logic on the right side of assertions.

Bad:

``kotlin
val startDato = LocalDate.of(2024, 1, 1)

result.opsjonMaksVarighet shouldBe startDato.plusYears(5).minusDays(1)``

Good:

``kotlin
val startDato = LocalDate.of(2024, 1, 1)

result.opsjonMaksVarighet shouldBe LocalDate.of(2028, 12, 31)``

This rule is included because derived expectations can repeat production bugs.

The same principle applies beyond dates:

* Use explicit expected amounts instead of recalculating them with the production formula.
* Use explicit expected collections instead of producing them with the same filtering or sorting.
* Do not call the same mapper, parser, factory, or helper in both test setup and expectation.
* It is fine to calculate input data during setup when the calculation is not also the behavior
  being verified.

## Keep tests deterministic

* Use fixed `LocalDate`, `LocalDateTime`, and `Instant` values. Avoid `now()` unless time itself is
  injected and controlled by the test.
* Use timestamps with persistence-supported precision when asserting complete persisted objects.
  PostgreSQL timestamps in this project are typically compared at microsecond precision.
* Use fixed IDs when their values matter to the assertion. Random IDs are acceptable only when
  identity is irrelevant and the test does not compare reconstructed objects containing them.
* Do not depend on test execution order or state left by another test.

## Test behavior at the correct boundary

* Domain tests specify aggregate invariants and state transitions.
* Service tests cover request-to-domain mapping, orchestration, persistence, and regressions caused
  by interactions between those layers.
* Route tests are reserved for HTTP-specific behavior such as serialization, authentication,
  status codes, and response contracts.
* When fixing a regression, add the smallest test that fails for the reported scenario. Add a
  lower-level domain test as well when the defect is a domain rule or calculation.

For a round-trip regression, construct the persisted state and submit the same externally visible
values through the real service mapping. Do not bypass the layer that caused the regression.

## Assert the complete meaningful outcome

* Assert the returned success or error.
* Assert changed state and important state that must remain unchanged.
* For persistence behavior, verify the reloaded or returned persisted representation when relevant.
* Cover the invalid counterpart of an invariant, not only the successful case.
* Avoid broad object equality when unrelated generated fields make the test fragile; assert the
  meaningful fields individually instead.

## Keep tests readable

* Structure tests as arrange, act, assert without comments that restate the code.
* Name tests after observable behavior using the domain language.
* Prefer one behavioral reason for failure per test.
* Keep fixture builders explicit at the point where a value is important to the scenario.
* Do not hide essential scenario data behind large generic fixtures.

## Verification

Run the narrowest affected tests first, then the affected module suites.