package no.nav.mulighetsrommet.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FakturanummerTest : FunSpec({
    context("parse") {
        test("skal godta godkjente fakturanummerformater") {
            Fakturanummer.parse("INV-0001")?.value shouldBe "INV-0001"
            Fakturanummer.parse("INV-0002")?.value shouldBe "INV-0002"
            Fakturanummer.parse("2026-001")?.value shouldBe "2026-001"
            Fakturanummer.parse("2026-002")?.value shouldBe "2026-002"
            Fakturanummer.parse("F-100")?.value shouldBe "F-100"
            Fakturanummer.parse("F-101")?.value shouldBe "F-101"
            Fakturanummer.parse("2026/1001")?.value shouldBe "2026/1001"
            Fakturanummer.parse("2026/1002")?.value shouldBe "2026/1002"
            Fakturanummer.parse("26.1001")?.value shouldBe "26.1001"
            Fakturanummer.parse("26.1002")?.value shouldBe "26.1002"
            Fakturanummer.parse("2026_1001")?.value shouldBe "2026_1001"
            Fakturanummer.parse("2026_1002")?.value shouldBe "2026_1002"
        }

        test("skal godta tall, små bokstaver og skilletegn før avsluttende siffer") {
            Fakturanummer.parse("01")?.value shouldBe "01"
            Fakturanummer.parse("1234567890123456789012345")?.value shouldBe "1234567890123456789012345"
            Fakturanummer.parse("inv-0001")?.value shouldBe "inv-0001"
            Fakturanummer.parse("INV-ABC-DEF-0001")?.value shouldBe "INV-ABC-DEF-0001"
            Fakturanummer.parse("-100")?.value shouldBe "-100"
            Fakturanummer.parse("INV--100")?.value shouldBe "INV--100"
        }

        test("skal trimme whitespace og returnere den normaliserte verdien") {
            Fakturanummer.parse("  INV-0001  ")?.value shouldBe "INV-0001"
            Fakturanummer.parse("\t2026-001\n")?.value shouldBe "2026-001"
        }

        test("skal godta makslengde på 30 tegn, også med whitespace rundt") {
            val maksLengde = "A".repeat(29) + "1"

            Fakturanummer.parse(maksLengde)?.value shouldBe maksLengde
            Fakturanummer.parse("  $maksLengde  ")?.value shouldBe maksLengde
        }

        test("skal avvise fakturanummer lengre enn 30 tegn") {
            Fakturanummer.parse("A".repeat(30) + "1") shouldBe null
        }

        test("skal avvise tomme verdier") {
            Fakturanummer.parse("") shouldBe null
            Fakturanummer.parse("   ") shouldBe null
            Fakturanummer.parse("\t\n") shouldBe null
        }

        test("skal avvise verdier som ikke avsluttes med et siffer") {
            Fakturanummer.parse("INV-ABC") shouldBe null
            Fakturanummer.parse("INV-") shouldBe null
            Fakturanummer.parse("INV.") shouldBe null
        }

        test("skal avvise bare skilletegn") {
            Fakturanummer.parse("-") shouldBe null
            Fakturanummer.parse("/._-") shouldBe null
        }

        test("skal avvise interne mellomrom, tabulatorer og linjeskift") {
            Fakturanummer.parse("INV 0001") shouldBe null
            Fakturanummer.parse("INV\t0001") shouldBe null
            Fakturanummer.parse("INV\n0001") shouldBe null
        }

        test("skal avvise ugyldige tegn og Unicode-tegn") {
            Fakturanummer.parse("INV+0001") shouldBe null
            Fakturanummer.parse("INV#0001") shouldBe null
            Fakturanummer.parse("١٢٣") shouldBe null
            Fakturanummer.parse("Ａ1") shouldBe null
        }
    }

    context("parseOrThrow") {
        test("skal returnere fakturanummer med trimmet verdi") {
            Fakturanummer.parseOrThrow("  INV-0001  ").value shouldBe "INV-0001"
        }

        test("skal kaste IllegalArgumentException med generisk melding for ugyldig verdi") {
            val exception = shouldThrow<IllegalArgumentException> {
                Fakturanummer.parseOrThrow("INV-ABC")
            }

            exception.message shouldBe "Ugyldig fakturanummer"
        }
    }
})
