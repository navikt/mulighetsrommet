package no.nav.mulighetsrommet.admin.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.domain.avtale.Opsjonsmodell
import no.nav.mulighetsrommet.validation.FieldError
import java.time.LocalDate

class OpsjonsmodellInputTest : FunSpec({
    fun toDomain(
        type: OpsjonsmodellType,
        opsjonMaksVarighet: LocalDate? = null,
        customOpsjonsmodellNavn: String? = null,
    ) = OpsjonsmodellInput(
        type = type,
        opsjonMaksVarighet = opsjonMaksVarighet,
        customOpsjonsmodellNavn = customOpsjonsmodellNavn,
    ).toDomain()

    test("ValgfriSluttdato opprettes uten avtaledatoer") {
        toDomain(OpsjonsmodellType.VALGFRI_SLUTTDATO)
            .shouldBeRight(Opsjonsmodell.ValgfriSluttdato)
    }

    test("IngenOpsjonsmulighet opprettes uten avtaledatoer") {
        toDomain(OpsjonsmodellType.INGEN_OPSJONSMULIGHET)
            .shouldBeRight(Opsjonsmodell.IngenOpsjonsmulighet)
    }

    test("standardmodeller ignorerer innsendt maks varighet") {
        toDomain(OpsjonsmodellType.TO_PLUSS_EN, opsjonMaksVarighet = LocalDate.of(2030, 1, 1))
            .shouldBeRight(Opsjonsmodell.ToPlussEn)
        toDomain(OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN)
            .shouldBeRight(Opsjonsmodell.ToPlussEnPlussEn)
        toDomain(OpsjonsmodellType.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN)
            .shouldBeRight(Opsjonsmodell.ToPlussEnPlussEnPlussEn)
    }

    test("ANNET krever maks varighet og et beskrivende navn") {
        toDomain(OpsjonsmodellType.ANNET).shouldBeLeft(
            listOf(
                FieldError("/opsjonsmodell", "Du må legge inn maks varighet for opsjonen"),
                FieldError("/opsjonsmodell", "Du må beskrive opsjonsmodellen"),
            ),
        )

        toDomain(
            type = OpsjonsmodellType.ANNET,
            opsjonMaksVarighet = LocalDate.of(2026, 1, 1),
            customOpsjonsmodellNavn = "Spesialordning",
        ).shouldBeRight(
            Opsjonsmodell.Annet(
                opsjonMaksVarighet = LocalDate.of(2026, 1, 1),
                customOpsjonsmodellNavn = "Spesialordning",
            ),
        )
    }
})
