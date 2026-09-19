package no.nav.mulighetsrommet.api.domain.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.mulighetsrommet.validation.FieldError
import java.time.LocalDate

class OpsjonsmodellTest : FunSpec({
    context("Opsjonsmodell.of") {
        val startDato = LocalDate.of(2024, 1, 1)

        test("ValgfriSluttdato tillater at sluttdato er null") {
            Opsjonsmodell.of(
                type = OpsjonsmodellType.VALGFRI_SLUTTDATO,
                startDato = startDato,
                sluttDato = null,
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeRight(Opsjonsmodell.ValgfriSluttdato(sluttDato = null))
        }

        test("IngenOpsjonsmulighet krever sluttdato") {
            Opsjonsmodell.of(
                type = OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
                startDato = startDato,
                sluttDato = null,
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeLeft(
                listOf(FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen")),
            )

            Opsjonsmodell.of(
                type = OpsjonsmodellType.INGEN_OPSJONSMULIGHET,
                startDato = startDato,
                sluttDato = startDato.plusYears(1),
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeRight(Opsjonsmodell.IngenOpsjonsmulighet(sluttDato = startDato.plusYears(1)))
        }

        test("TO_PLUSS_EN/-EN_PLUSS_EN/-EN_PLUSS_EN_PLUSS_EN regner maks varighet ut fra startdato") {
            val toPlussEn = Opsjonsmodell.of(
                type = OpsjonsmodellType.TO_PLUSS_EN,
                startDato = startDato,
                sluttDato = null,
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeRight().shouldBeInstanceOf<Opsjonsmodell.ToPlussEn>()

            toPlussEn.opsjonMaksVarighet shouldBe LocalDate.of(2026, 12, 31)

            val toPlussEnPlussEn = Opsjonsmodell.of(
                type = OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN,
                startDato = startDato,
                sluttDato = null,
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeRight().shouldBeInstanceOf<Opsjonsmodell.ToPlussEnPlussEn>()

            toPlussEnPlussEn.opsjonMaksVarighet shouldBe LocalDate.of(2027, 12, 31)

            val toPlussEnPlussEnPlussEn = Opsjonsmodell.of(
                type = OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
                startDato = startDato,
                sluttDato = null,
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeRight().shouldBeInstanceOf<Opsjonsmodell.ToPlussEnPlussEnPlussEn>()

            toPlussEnPlussEnPlussEn.opsjonMaksVarighet shouldBe LocalDate.of(2028, 12, 31)
        }

        test("ANNET krever sluttdato, maks varighet og et beskrivende navn") {
            Opsjonsmodell.of(
                type = OpsjonsmodellType.ANNET,
                startDato = startDato,
                sluttDato = null,
                opsjonMaksVarighet = null,
                customOpsjonsmodellNavn = null,
            ).shouldBeLeft(
                listOf(
                    FieldError("/sluttDato", "Du må legge inn sluttdato for avtalen"),
                    FieldError("/opsjonsmodell", "Du må legge inn maks varighet for opsjonen"),
                    FieldError("/opsjonsmodell", "Du må beskrive opsjonsmodellen"),
                ),
            )

            Opsjonsmodell.of(
                type = OpsjonsmodellType.ANNET,
                startDato = startDato,
                sluttDato = startDato.plusYears(1),
                opsjonMaksVarighet = startDato.plusYears(2),
                customOpsjonsmodellNavn = "Spesialordning",
            ).shouldBeRight(
                Opsjonsmodell.Annet(
                    sluttDato = startDato.plusYears(1),
                    opsjonMaksVarighet = startDato.plusYears(2),
                    customOpsjonsmodellNavn = "Spesialordning",
                ),
            )
        }
    }
})
