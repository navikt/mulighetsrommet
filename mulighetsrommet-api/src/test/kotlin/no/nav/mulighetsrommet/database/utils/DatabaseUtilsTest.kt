package no.nav.mulighetsrommet.database.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery

class DatabaseUtilsTest : FunSpec({
    context("toFTSPrefixQuery") {
        test("lopenummer") {
            "2015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
        }

        test("splits by word") {
            "bodø aft".toFTSPrefixQuery() shouldBe "bodø:* & aft:*"
        }

        test("strips illegal chars") {
            "(2015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2015:/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2015/1213`".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2*015/1213`".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2)015/1213`".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2?015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2&015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "2\\015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "!2015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "'2015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
            "\"2015/1213".toFTSPrefixQuery() shouldBe "2015/1213:*"
        }
    }
})
