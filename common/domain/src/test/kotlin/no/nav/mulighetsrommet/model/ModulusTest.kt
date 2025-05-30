package no.nav.mulighetsrommet.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ModulusTest : FunSpec({
    test("mod 10 alg") {
        Modulus.hasValidControlDigit("2345676", Modulus.Algorithm.MOD10) shouldBe true
        Modulus.hasValidControlDigit("23456767", Modulus.Algorithm.MOD10) shouldBe true
        Modulus.hasValidControlDigit("0044555258", Modulus.Algorithm.MOD10) shouldBe true
        Modulus.hasValidControlDigit("2345674", Modulus.Algorithm.MOD10) shouldBe false
        Modulus.hasValidControlDigit("0044555251", Modulus.Algorithm.MOD10) shouldBe false
    }

    test("mod 11 alg") {
        Modulus.hasValidControlDigit("0365327", Modulus.Algorithm.MOD11) shouldBe true
        Modulus.hasValidControlDigit("12345678903", Modulus.Algorithm.MOD11) shouldBe true
        Modulus.hasValidControlDigit("036536-", Modulus.Algorithm.MOD11) shouldBe true
        Modulus.hasValidControlDigit("1677036255", Modulus.Algorithm.MOD11) shouldBe false
    }

    test("ekte kid nummer") {
        "006402710013".let {
            Modulus.hasValidControlDigit(it, Modulus.Algorithm.MOD10) ||
                Modulus.hasValidControlDigit(it, Modulus.Algorithm.MOD11) shouldBe true
        }
        "0094026358".let {
            Modulus.hasValidControlDigit(it, Modulus.Algorithm.MOD10) ||
                Modulus.hasValidControlDigit(it, Modulus.Algorithm.MOD11) shouldBe true
        }
        "0004614992".let {
            Modulus.hasValidControlDigit(it, Modulus.Algorithm.MOD10) ||
                Modulus.hasValidControlDigit(it, Modulus.Algorithm.MOD11) shouldBe true
        }
    }
})
