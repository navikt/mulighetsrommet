package no.nav.mulighetsrommet.validation

@OptIn(ExperimentalContracts::class)
class FieldValidatorTest : FunSpec({
    test("poc") {
        val a = validation {
            val g: Int? = null

            requireValid(g != null) {
                FieldError.of("asdf")
            }

            g + 7
        }

        a.shouldBeLeft().should {
            it shouldHaveSize 1
        }
    }
})
