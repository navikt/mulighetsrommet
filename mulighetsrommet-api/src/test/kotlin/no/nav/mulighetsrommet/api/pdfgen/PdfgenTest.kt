package no.nav.mulighetsrommet.api.pdfgen

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravDeltakelse
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.tilsagn.model.ArrangorflateTilsagn
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class PdfgenTest : FunSpec({
    test("refusjon-kvittering") {
        val tilsagn = listOf(
            ArrangorflateTilsagn(
                id = UUID.randomUUID(),
                gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                    navn = AFT1.navn,
                ),
                tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                    navn = TiltakstypeFixtures.AFT.navn,
                ),
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                arrangor = ArrangorflateTilsagn.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                ),
                beregning = Prismodell.TilsagnBeregning.Fri(123),
            ),
        )

        val refusjonsKravAft = RefusjonKravAft(
            id = UUID.randomUUID(),
            status = RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
            fristForGodkjenning = LocalDateTime.now(),
            tiltakstype = RefusjonskravDto.Tiltakstype(
                navn = "AFT",
            ),
            gjennomforing = RefusjonskravDto.Gjennomforing(
                id = AFT1.id,
                navn = AFT1.navn,
            ),
            arrangor = RefusjonskravDto.Arrangor(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "Navn",
                slettet = false,
            ),
            deltakelser = listOf(
                RefusjonKravDeltakelse(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusDays(2),
                    forstePeriodeStartDato = LocalDate.now(),
                    sistePeriodeSluttDato = LocalDate.now(),
                    sistePeriodeDeltakelsesprosent = 100.0,
                    manedsverk = 1.0,
                    person = RefusjonKravDeltakelse.Person(
                        navn = "Donald DUck",
                        fodselsdato = LocalDate.now(),
                        fodselsaar = 2000,
                    ),
                    veileder = null,
                ),
            ),
            beregning = RefusjonKravAft.Beregning(
                periodeStart = LocalDate.now(),
                periodeSlutt = LocalDate.now(),
                antallManedsverk = 1.0,
                belop = 100,
                digest = "asdf",
            ),
            betalingsinformasjon = RefusjonskravDto.Betalingsinformasjon(
                kid = null,
                kontonummer = Kontonummer("12312312312"),
            ),
        )

        Pdfgen.Aft.refusjonKvittering(refusjonsKravAft, tilsagn)
    }

    test("refusjon-journalpost") {
        val tilsagn = listOf(
            ArrangorflateTilsagn(
                id = UUID.randomUUID(),
                gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                    navn = AFT1.navn,
                ),
                tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                    navn = TiltakstypeFixtures.AFT.navn,
                ),
                periodeStart = LocalDate.of(2023, 1, 1),
                periodeSlutt = LocalDate.of(2023, 2, 1),
                arrangor = ArrangorflateTilsagn.Arrangor(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                ),
                beregning = Prismodell.TilsagnBeregning.AFT(
                    belop = 450000,
                    sats = 20501,
                    antallPlasser = 23,
                    periodeStart = LocalDate.now().minusMonths(3),
                    periodeSlutt = LocalDate.now().plusMonths(3),
                ),
            ),
        )

        val refusjonsKravAft = RefusjonKravAft(
            id = UUID.randomUUID(),
            status = RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
            fristForGodkjenning = LocalDateTime.now(),
            tiltakstype = RefusjonskravDto.Tiltakstype(
                navn = "AFT",
            ),
            gjennomforing = RefusjonskravDto.Gjennomforing(
                id = AFT1.id,
                navn = AFT1.navn,
            ),
            arrangor = RefusjonskravDto.Arrangor(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "Fretex AS",
                slettet = false,
            ),
            deltakelser = listOf(
                RefusjonKravDeltakelse(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusDays(2),
                    forstePeriodeStartDato = LocalDate.now(),
                    sistePeriodeSluttDato = LocalDate.now(),
                    sistePeriodeDeltakelsesprosent = 100.0,
                    manedsverk = 1.0,
                    person = RefusjonKravDeltakelse.Person(
                        navn = "Donald DUck",
                        fodselsdato = LocalDate.of(1945, 12, 8),
                        fodselsaar = 2000,
                    ),
                    veileder = null,
                ),
                RefusjonKravDeltakelse(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusDays(20),
                    forstePeriodeStartDato = LocalDate.now(),
                    sistePeriodeSluttDato = LocalDate.now(),
                    sistePeriodeDeltakelsesprosent = 100.0,
                    manedsverk = 1.0,
                    person = RefusjonKravDeltakelse.Person(
                        navn = "Adressebeskyttet",
                        fodselsaar = null,
                        fodselsdato = null,
                    ),
                    veileder = null,
                ),
                RefusjonKravDeltakelse(
                    id = UUID.randomUUID(),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusDays(20),
                    forstePeriodeStartDato = LocalDate.now(),
                    sistePeriodeSluttDato = LocalDate.now(),
                    sistePeriodeDeltakelsesprosent = 100.0,
                    manedsverk = .5,
                    person = RefusjonKravDeltakelse.Person(
                        navn = "Onkel Skrue",
                        fodselsaar = null,
                        fodselsdato = LocalDate.of(1923, 1, 4),
                    ),
                    veileder = null,
                ),
            ),
            beregning = RefusjonKravAft.Beregning(
                periodeStart = LocalDate.now(),
                periodeSlutt = LocalDate.now().plusMonths(1),
                antallManedsverk = 2.5,
                belop = 100,
                digest = "asdf",
            ),
            betalingsinformasjon = RefusjonskravDto.Betalingsinformasjon(
                kid = null,
                kontonummer = Kontonummer("12312312312"),
            ),
        )

        // Sjekker kun at ting ikke krasjer
        val pdf = Pdfgen.Aft.refusjonJournalpost(refusjonsKravAft, tilsagn)

        // For å iterere på pdf'en kan man kjøre denne testen og kommentere ut linjene under
        // val file = File("/<path>/refusjonskrav.pdf")
        // file.writeBytes(pdf)
    }
})
