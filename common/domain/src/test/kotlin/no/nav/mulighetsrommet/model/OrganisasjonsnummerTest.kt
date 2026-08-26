package no.nav.mulighetsrommet.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OrganisasjonsnummerTest : FunSpec({
    context("isValid") {
        test("skal være true for gyldig organisasjonsnummer med 9 siffer") {
            Organisasjonsnummer.isValid("123456789") shouldBe true
        }

        test("skal være false for organisasjonsnummer med whitespace") {
            Organisasjonsnummer.isValid("123 456 789") shouldBe false
            Organisasjonsnummer.isValid(" 123456789 ") shouldBe false
        }

        test("skal være false når det ikke er 9 siffer") {
            Organisasjonsnummer.isValid("12345678") shouldBe false
            Organisasjonsnummer.isValid("1234567890") shouldBe false
            Organisasjonsnummer.isValid("") shouldBe false
        }

        test("skal være false når det inneholder ikke-numeriske tegn") {
            Organisasjonsnummer.isValid("12345678a") shouldBe false
            Organisasjonsnummer.isValid("123-45678") shouldBe false
        }
    }

    context("parse") {
        test("skal returnere Organisasjonsnummer for gyldig verdi") {
            Organisasjonsnummer.parse("123456789") shouldBe Organisasjonsnummer("123456789")
        }

        test("skal normalisere bort whitespace") {
            Organisasjonsnummer.parse(" 123 456 789 ") shouldBe Organisasjonsnummer("123456789")
        }

        test("skal returnere null for ugyldig verdi") {
            Organisasjonsnummer.parse("12345678") shouldBe null
            Organisasjonsnummer.parse("abcdefghi") shouldBe null
        }
    }

    context("constructor") {
        test("skal opprette instans for gyldig organisasjonsnummer") {
            Organisasjonsnummer("123456789").value shouldBe "123456789"
        }

        test("skal kaste exception for ugyldig organisasjonsnummer") {
            shouldThrow<IllegalArgumentException> {
                Organisasjonsnummer("12345678")
            }
        }
    }

    test("toString returnerer verdien") {
        Organisasjonsnummer("123456789").toString() shouldBe "123456789"
    }
})
