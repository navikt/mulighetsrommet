package no.nav.mulighetsrommet.arena.adapter.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalTime

class ArenaUtilsTest : FunSpec({
    test("parseKlokketid") {
        ArenaUtils.parseKlokketid("0900") shouldBe LocalTime.of(9, 0)
        ArenaUtils.parseKlokketid("1255") shouldBe LocalTime.of(12, 55)
        ArenaUtils.parseKlokketid("9") shouldBe LocalTime.of(9, 0)
        ArenaUtils.parseKlokketid("12") shouldBe LocalTime.of(12, 0)
        ArenaUtils.parseKlokketid("9.30") shouldBe LocalTime.of(9, 30)
        ArenaUtils.parseKlokketid("725") shouldBe LocalTime.of(7, 25)
        ArenaUtils.parseKlokketid("7:25") shouldBe LocalTime.of(7, 25)
        ArenaUtils.parseKlokketid("7.25") shouldBe LocalTime.of(7, 25)
        ArenaUtils.parseKlokketid("07.25") shouldBe LocalTime.of(7, 25)
        ArenaUtils.parseKlokketid("kl 10") shouldBe LocalTime.of(10, 0)
        ArenaUtils.parseKlokketid("10 10") shouldBe LocalTime.of(10, 10)

        ArenaUtils.parseKlokketid(null) shouldBe null
        ArenaUtils.parseKlokketid("x77xxxx") shouldBe null
        ArenaUtils.parseKlokketid("e a") shouldBe null
        ArenaUtils.parseKlokketid("0990") shouldBe null
        ArenaUtils.parseKlokketid("18092") shouldBe null
        ArenaUtils.parseKlokketid("sms") shouldBe null
        ArenaUtils.parseKlokketid(".") shouldBe null
        ArenaUtils.parseKlokketid("09>:0") shouldBe null
        ArenaUtils.parseKlokketid("019.0") shouldBe null
    }
})
