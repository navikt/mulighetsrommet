package no.nav.mulighetsrommet.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class KidTest : FunSpec({
    test("tom streng behandles som fravær av kid") {
        Kid.parse("").shouldBeNull()
    }

    test("blank streng behandles som fravær av kid") {
        Kid.parse("   ").shouldBeNull()
    }

    test("trimmer whitespace før validering") {
        Kid.parse(" 116 ").shouldNotBeNull()
    }

    test("godtar med kontrollsiffer") {
        Kid.parse("2345676").shouldNotBeNull()
    }

    test("godtar '-' som kontrolltegn på slutten") {
        Kid.parse("036536-").shouldNotBeNull()
    }

    test("avviser '-' som ikke står på slutten") {
        Kid.parse("-36536").shouldBeNull()
        Kid.parse("0365-36").shouldBeNull()
    }

    test("avviser ugyldige tegn") {
        Kid.parse("123a").shouldBeNull()
    }

    test("avviser for kort format") {
        Kid.parse("11").shouldBeNull()
    }

    test("avviser for lang streng") {
        Kid.parse("1".repeat(26)).shouldBeNull()
    }
})
