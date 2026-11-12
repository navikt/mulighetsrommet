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