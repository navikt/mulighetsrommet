package no.nav.mulighetsrommet.api.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.util.*

class OpsjonLoggValidatorTest : FunSpec({
    val avtale = AvtaleDto(
        id = UUID.randomUUID(),
        tiltakstype = AvtaleDto.Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakskode = Tiltakskode.OPPFOLGING,
        ),
        navn = "Avtale for opsjoner",
        avtalenummer = "24/123",
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = AvtaleDto.ArrangorHovedenhet(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Fretex AS",
            slettet = false,
            underenheter = emptyList(),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2024, 7, 5),
        sluttDato = LocalDate.of(2024, 7, 5).plusYears(2),
        arenaAnsvarligEnhet = ArenaNavEnhet(
            navn = "Nav Oslo",
            enhetsnummer = "0100",
        ),
        avtaletype = Avtaletype.AVTALE,
        status = AvtaleStatus.AKTIV,
        prisbetingelser = null,
        administratorer = emptyList(),
        antallPlasser = 10,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontorstruktur = emptyList(),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodellData = OpsjonsmodellData(
            opsjonMaksVarighet = LocalDate.of(2024, 7, 5).plusYears(5),
            opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN_PLUSS_EN_PLUSS_EN,
            customOpsjonsmodellNavn = null,
        ),
        opsjonerRegistrert = emptyList(),
        utdanningslop = null,
        prismodell = null,
    )

    test("Skal kaste en feil hvis opsjonsmodell ikke finnes") {
        val avtaleUtenOpsjonsmodell = avtale.copy(opsjonsmodellData = null)
        val entry = OpsjonLoggEntry(
            avtaleId = UUID.randomUUID(),
            sluttdato = null,
            forrigeSluttdato = null,
            status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
            registretDato = LocalDate.of(2024, 7, 6),
            registrertAv = NavIdent("M123456"),
        )
        OpsjonLoggValidator.validate(entry, avtaleUtenOpsjonsmodell).shouldBeLeft().shouldContainAll(
            FieldError.of(OpsjonsmodellData::opsjonsmodell, "Kan ikke registrer opsjon uten en opsjonsmodell"),
        )
    }

    test("Skal kaste en feil hvis status for entry er UTLØST_OPSJON og ny sluttdato er senere enn maks varighet for opsjonsmodellen") {
        val avtale2Pluss1 = avtale.copy(
            sluttDato = LocalDate.of(2024, 7, 5).plusYears(3),
            opsjonsmodellData = OpsjonsmodellData(
                opsjonMaksVarighet = LocalDate.of(2024, 7, 5).plusYears(3),
                opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
                customOpsjonsmodellNavn = null,
            ),
        )

        val entry = OpsjonLoggEntry(
            avtaleId = UUID.randomUUID(),
            sluttdato = LocalDate.of(2027, 7, 6),
            forrigeSluttdato = LocalDate.of(2026, 7, 6),
            status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
            registretDato = LocalDate.of(2024, 7, 6),
            registrertAv = NavIdent("M123456"),
        )

        OpsjonLoggValidator.validate(entry, avtale2Pluss1).shouldBeLeft().shouldContainAll(
            FieldError.of(
                OpsjonLoggEntry::sluttdato,
                "Ny sluttdato er forbi maks varighet av avtalen",
            ),
        )
    }

    test("Skal kaste en feil hvis status for entry er UTLØST_OPSJON og forrige sluttdato mangler") {
        val avtale2Pluss1 = avtale.copy(
            sluttDato = LocalDate.of(2024, 7, 5).plusYears(3),
            opsjonsmodellData = OpsjonsmodellData(
                opsjonMaksVarighet = LocalDate.of(2024, 7, 5).plusYears(1),
                opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
                customOpsjonsmodellNavn = null,
            ),
        )

        val entry = OpsjonLoggEntry(
            avtaleId = UUID.randomUUID(),
            sluttdato = LocalDate.of(2025, 7, 6),
            forrigeSluttdato = null,
            status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
            registretDato = LocalDate.of(2024, 7, 6),
            registrertAv = NavIdent("M123456"),
        )

        OpsjonLoggValidator.validate(entry, avtale2Pluss1).shouldBeLeft().shouldContainAll(
            FieldError.of(
                OpsjonLoggEntry::forrigeSluttdato,
                "Forrige sluttdato må være satt",
            ),
        )
    }

    test("Skal kaste en feil hvis status for entry er UTLØST_OPSJON og det allerede er registrert at det ikke skal utløses opsjon for avtalen") {
        val avtale2Pluss1 = avtale.copy(
            sluttDato = LocalDate.of(2024, 7, 5).plusYears(3),
            opsjonsmodellData = OpsjonsmodellData(
                opsjonMaksVarighet = LocalDate.of(2024, 7, 5).plusYears(3),
                opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
                customOpsjonsmodellNavn = null,
            ),
            opsjonerRegistrert = listOf(
                AvtaleDto.OpsjonLoggRegistrert(
                    id = UUID.randomUUID(),
                    status = OpsjonLoggRequest.OpsjonsLoggStatus.SKAL_IKKE_UTLØSE_OPSJON,
                    registrertDato = LocalDate.of(2024, 8, 8),
                    sluttDato = null,
                    forrigeSluttdato = null,
                ),
            ),
        )

        val entry = OpsjonLoggEntry(
            avtaleId = UUID.randomUUID(),
            sluttdato = LocalDate.of(2027, 7, 6),
            forrigeSluttdato = LocalDate.of(2026, 7, 6),
            status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
            registretDato = LocalDate.of(2024, 7, 6),
            registrertAv = NavIdent("M123456"),
        )

        OpsjonLoggValidator.validate(entry, avtale2Pluss1).shouldBeLeft().shouldContainAll(
            FieldError.of(
                OpsjonLoggEntry::status,
                "Kan ikke utløse opsjon for avtale som har en opsjon som ikke skal utløses",
            ),
        )
    }

    test("Skal ikke kaste en feil hvis status for entry er UTLØST_OPSJON og ny sluttdato er før enn maks varighet for opsjonsmodellen") {
        val avtale2Pluss1 = avtale.copy(
            sluttDato = LocalDate.of(2024, 7, 5).plusYears(3),
            opsjonsmodellData = OpsjonsmodellData(
                opsjonMaksVarighet = LocalDate.of(2024, 7, 5).plusYears(3),
                opsjonsmodell = Opsjonsmodell.TO_PLUSS_EN,
                customOpsjonsmodellNavn = null,
            ),
        )

        val entry = OpsjonLoggEntry(
            avtaleId = UUID.randomUUID(),
            sluttdato = LocalDate.of(2025, 7, 6),
            forrigeSluttdato = LocalDate.of(2024, 7, 6),
            status = OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST,
            registretDato = LocalDate.of(2024, 7, 6),
            registrertAv = NavIdent("M123456"),
        )

        OpsjonLoggValidator.validate(entry, avtale2Pluss1).shouldBeRight()
    }
})
