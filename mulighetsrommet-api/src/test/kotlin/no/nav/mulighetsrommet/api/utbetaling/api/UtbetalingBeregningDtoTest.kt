package no.nav.mulighetsrommet.api.utbetaling.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.navenhet.toDto
import no.nav.mulighetsrommet.api.utbetaling.DeltakerPersonaliaMedGeografiskEnhet
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling.Arrangor
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling.Betalingsinformasjon
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingBeregningDtoTest : FunSpec({
    val deltakelseId = UUID.randomUUID()
    val utbetaling = Utbetaling(
        id = UUID.randomUUID(),
        innsender = null,
        tiltakstype = Utbetaling.Tiltakstype(
            navn = TiltakstypeFixtures.AFT.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
        ),
        gjennomforing = Utbetaling.Gjennomforing(
            navn = GjennomforingFixtures.AFT1.navn,
            id = GjennomforingFixtures.AFT1.id,
        ),
        arrangor = Arrangor(
            id = ArrangorFixtures.underenhet1.id,
            navn = ArrangorFixtures.underenhet1.navn,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            slettet = false,
        ),
        beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
            input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                periode = Periode.forMonthOf(LocalDate.now()),
                sats = 10,
                stengt = emptySet(),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltakelseId,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(Periode.forMonthOf(LocalDate.now()), 100.0),
                        ),
                    ),
                ),
            ),
            output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 10,
                deltakelser = setOf(
                    DeltakelseManedsverk(
                        deltakelseId = deltakelseId,
                        manedsverk = 1.0,
                    ),
                ),
            ),
        ),
        betalingsinformasjon = Betalingsinformasjon(null, null),
        journalpostId = null,
        periode = Periode.forMonthOf(LocalDate.now()),
        godkjentAvArrangorTidspunkt = null,
        createdAt = LocalDateTime.now(),
        beskrivelse = null,
        begrunnelseMindreBetalt = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        status = UtbetalingStatusType.GENERERT,
    )

    val personalia = DeltakerPersonaliaMedGeografiskEnhet(
        deltakerId = deltakelseId,
        norskIdent = NorskIdent("01010199999"),
        navn = "Normann, Ola",
        erSkjermet = false,
        adressebeskyttelse = PdlGradering.UGRADERT,
        oppfolgingEnhet = NavEnhetFixtures.Sel.toDto(),
        geografiskEnhet = NavEnhetFixtures.Gjovik.toDto(),
        region = NavEnhetFixtures.Innlandet.toDto(),
    )

    context("skjermet og adressebeskyttet") {
        test("ikke skjermet eller adressebeskyttet") {
            val row = UtbetalingBeregningDto.from(
                utbetaling,
                listOf(DeltakelseManedsverk(deltakelseId = deltakelseId, manedsverk = 1.0) to personalia),
                regioner = emptyList(),
            ).deltakerTableData.rows[0]

            row["navn"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Normann, Ola"
            row["oppfolgingEnhet"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Nav Sel"
            row["geografiskEnhet"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Nav Gjøvik"
            row["region"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Nav Innlandet"
        }

        test("adressebeskyttet navn") {
            for (gradering in PdlGradering.entries) {
                if (gradering == PdlGradering.UGRADERT) {
                    continue
                }
                val row = UtbetalingBeregningDto.from(
                    utbetaling,
                    listOf(
                        DeltakelseManedsverk(deltakelseId = deltakelseId, manedsverk = 1.0) to personalia.copy(
                            adressebeskyttelse = gradering,
                        ),
                    ),
                    regioner = emptyList(),
                ).deltakerTableData.rows[0]
                row["navn"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Adressebeskyttet"
                row["oppfolgingEnhet"].shouldBeNull()
                row["geografiskEnhet"].shouldBeNull()
                row["region"].shouldBeNull()
            }
        }

        test("skjermet navn") {
            val row = UtbetalingBeregningDto.from(
                utbetaling,
                listOf(
                    DeltakelseManedsverk(deltakelseId = deltakelseId, manedsverk = 1.0) to personalia.copy(
                        erSkjermet = true,
                    ),
                ),
                regioner = emptyList(),
            ).deltakerTableData.rows[0]
            row["navn"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Skjermet"
            row["oppfolgingEnhet"].shouldBeNull()
            row["geografiskEnhet"].shouldBeNull()
            row["region"].shouldBeNull()
        }

        test("adressebeskyttelse tar presedens over skjerming") {
            val row = UtbetalingBeregningDto.from(
                utbetaling,
                listOf(
                    DeltakelseManedsverk(deltakelseId = deltakelseId, manedsverk = 1.0) to personalia.copy(
                        erSkjermet = true,
                        adressebeskyttelse = PdlGradering.STRENGT_FORTROLIG_UTLAND,
                    ),
                ),
                regioner = emptyList(),
            ).deltakerTableData.rows[0]
            row["navn"].shouldBeTypeOf<DataElement.Text>().value shouldBe "Adressebeskyttet"
            row["oppfolgingEnhet"].shouldBeNull()
            row["geografiskEnhet"].shouldBeNull()
            row["region"].shouldBeNull()
        }
    }
})
