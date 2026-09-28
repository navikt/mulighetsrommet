package no.nav.mulighetsrommet.ereg

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PostnummerregisterTest : FunSpec({
    test("skal slå opp poststed for kjente postnumre") {
        Postnummerregister.poststed("0170") shouldBe "OSLO"
        Postnummerregister.poststed("7374") shouldBe "RØROS"
        Postnummerregister.poststed("5008") shouldBe "BERGEN"
    }

    test("skal returnere null for ukjent postnummer") {
        Postnummerregister.poststed("9999") shouldBe null
    }
})
