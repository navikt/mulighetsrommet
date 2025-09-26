package no.nav.mulighetsrommet.api.validation

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import no.nav.mulighetsrommet.api.responses.FieldError
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
class ValidationDslTest : FunSpec({
    test("poc") {
        val a = validation {
            val g: Int? = null

            requireValid(g != null) {
                FieldError.root("asdf")
            }

            val h = g + 7
        }

        a.shouldBeLeft().should {
            it shouldHaveSize 1
        }
    }
})
