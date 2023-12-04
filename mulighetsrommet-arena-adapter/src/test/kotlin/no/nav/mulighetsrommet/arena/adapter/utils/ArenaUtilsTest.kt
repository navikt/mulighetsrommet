package no.nav.mulighetsrommet.arena.adapter.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArenaUtilsTest : FunSpec({
    test("parseKlokketid") {
        ArenaUtils.parseKlokketid(null) shouldBe null
        ArenaUtils.parseKlokketid("0900") shouldBe (9 to 0)
        ArenaUtils.parseKlokketid("1255") shouldBe (12 to 55)
        ArenaUtils.parseKlokketid("9") shouldBe (9 to 0)
        ArenaUtils.parseKlokketid("12") shouldBe (12 to 0)
        ArenaUtils.parseKlokketid("9.30") shouldBe (9 to 30)
        ArenaUtils.parseKlokketid("725") shouldBe (7 to 25)
        ArenaUtils.parseKlokketid("7:25") shouldBe (7 to 25)
        ArenaUtils.parseKlokketid("7.25") shouldBe (7 to 25)
        ArenaUtils.parseKlokketid("07.25") shouldBe (7 to 25)
    }
})
