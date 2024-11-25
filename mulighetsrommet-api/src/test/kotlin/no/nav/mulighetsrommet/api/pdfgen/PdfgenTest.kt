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

        Pdfgen.refusjonKvittering(refusjonsKravAft, tilsagn)
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

        Pdfgen.refusjonJournalpost(refusjonsKravAft, tilsagn)
    }
})
