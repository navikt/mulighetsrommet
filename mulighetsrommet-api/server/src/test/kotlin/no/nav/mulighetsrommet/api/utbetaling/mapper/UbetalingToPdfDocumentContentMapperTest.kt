package no.nav.mulighetsrommet.api.utbetaling.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnSummary
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse.BeregnetPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID

class UbetalingToPdfDocumentContentMapperTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val gjennomforing = GjennomforingFixtures.createGjennomforingAvtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.OPPFOLGING,
        periode = Periode.forYear(2025),
    )

    val deltaker1Id = UUID.randomUUID()
    val deltaker2Id = UUID.randomUUID()
    val deltaker3Id = UUID.randomUUID()
    val deltaker4Id = UUID.randomUUID()
    val deltaker5Id = UUID.randomUUID()

    val sats = 1000.NOK
    val utbetalingFastSats = Utbetaling(
        id = UUID.randomUUID(),
        status = UtbetalingStatusType.FERDIG_BEHANDLET,
        utbetalesTidligstTidspunkt = null,
        createdAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        updatedAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        tiltakstype = Utbetaling.Tiltakstype("Oppfølging", Tiltakskode.OPPFOLGING),
        gjennomforing = Utbetaling.Gjennomforing(
            id = gjennomforing.id,
            lopenummer = gjennomforing.lopenummer,
        ),
        arrangor = Utbetaling.Arrangor(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Nav",
            slettet = false,
        ),
        korreksjon = null,
        innsending = Utbetaling.Innsending(
            tidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        ),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningFastSatsPerBenyttetPlassPerManed(
            input = UtbetalingBeregningFastSatsPerBenyttetPlassPerManed.Input(
                satser = setOf(SatsPeriode(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), sats)),
                stengt = setOf(
                    StengtPeriode(
                        periode = Periode(LocalDate.of(2025, 1, 7), LocalDate.of(2025, 1, 14)),
                        beskrivelse = "Stengt for ferie",
                    ),
                ),
                deltakelser = setOf(
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltaker1Id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltaker2Id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltaker3Id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15)),
                                deltakelsesprosent = 50.0,
                            ),
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltaker4Id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15)),
                                deltakelsesprosent = 50.0,
                            ),
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                    DeltakelseDeltakelsesprosentPerioder(
                        deltakelseId = deltaker5Id,
                        perioder = listOf(
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15)),
                                deltakelsesprosent = 50.0,
                            ),
                            DeltakelsesprosentPeriode(
                                periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1)),
                                deltakelsesprosent = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
            output = UtbetalingBeregningFastSatsPerBenyttetPlassPerManed.Output(
                pris = 100.NOK,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker1Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 1.0,
                                sats = sats,
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker2Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 1.0,
                                sats = sats,
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker3Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 0.75,
                                sats = sats,
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker4Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 0.75,
                                sats = sats,
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker5Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 0.75,
                                sats = sats,
                                periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(kontonummer = Kontonummer("12345678901"), null),
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        kommentar = null,
        journalpostId = null,
        begrunnelseMindreBetalt = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        avbruttBegrunnelse = null,
        avbruttTidspunkt = null,
        blokkeringer = emptySet(),
        avbrytelse = null,
    )

    val utbetalingPrisPerTimeOppfolging = Utbetaling(
        id = UUID.randomUUID(),
        status = UtbetalingStatusType.FERDIG_BEHANDLET,
        utbetalesTidligstTidspunkt = null,
        createdAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        updatedAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        tiltakstype = Utbetaling.Tiltakstype("Oppfolging", Tiltakskode.OPPFOLGING),
        gjennomforing = Utbetaling.Gjennomforing(
            id = gjennomforing.id,
            lopenummer = gjennomforing.lopenummer,
        ),
        arrangor = Utbetaling.Arrangor(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Nav",
            slettet = false,
        ),
        korreksjon = null,
        innsending = Utbetaling.Innsending(
            tidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        ),
        valuta = Valuta.NOK,
        beregning = UtbetalingBeregningAvtaltPrisPerTimeOppfolging(
            input = UtbetalingBeregningAvtaltPrisPerTimeOppfolging.Input(
                satser = setOf(SatsPeriode(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), 34.NOK)),
                stengt = setOf(
                    StengtPeriode(
                        periode = Periode(LocalDate.of(2025, 1, 7), LocalDate.of(2025, 1, 14)),
                        beskrivelse = "Stengt for ferie",
                    ),
                ),
                deltakelser = setOf(
                    DeltakelsePeriode(
                        deltakelseId = deltaker1Id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    ),
                    DeltakelsePeriode(
                        deltakelseId = deltaker2Id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    ),
                    DeltakelsePeriode(
                        deltakelseId = deltaker3Id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    ),
                    DeltakelsePeriode(
                        deltakelseId = deltaker4Id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    ),
                    DeltakelsePeriode(
                        deltakelseId = deltaker5Id,
                        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                    ),
                ),
                pris = 100.NOK,
            ),
            output = UtbetalingBeregningAvtaltPrisPerTimeOppfolging.Output(
                pris = 100.NOK,
            ),
        ),
        betalingsinformasjon = Betalingsinformasjon.BBan(kontonummer = Kontonummer("12345678901"), null),
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        kommentar = null,
        journalpostId = null,
        begrunnelseMindreBetalt = null,
        avbruttBegrunnelse = null,
        avbruttTidspunkt = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        blokkeringer = emptySet(),
        avbrytelse = null,
    )

    val personalia = listOf(
        Personalia(
            deltaker1Id,
            navn = "Ola Skjermet",
            norskIdent = NorskIdent("01010199999"),
            oppfolgingEnhet = null,
            geografiskEnhet = null,
            gradering = Gradering.SKJERMING,
            region = null,
            avvistGrunn = null,
        ),
        Personalia(
            deltaker2Id,
            navn = "Ola Nordmann",
            norskIdent = NorskIdent("01010199999"),
            oppfolgingEnhet = null,
            gradering = Gradering.UGRADERT,
            geografiskEnhet = null,
            avvistGrunn = null,
            region = null,
        ),
        Personalia(
            deltaker3Id,
            navn = "Kari Nordmann",
            norskIdent = NorskIdent("01010199998"),
            oppfolgingEnhet = null,
            gradering = Gradering.FORTROLIG_ADRESSE,
            geografiskEnhet = null,
            region = null,
            avvistGrunn = null,
        ),
        Personalia(
            deltaker4Id,
            navn = "Kari Nordmann",
            norskIdent = NorskIdent("01010199998"),
            oppfolgingEnhet = null,
            gradering = Gradering.STRENGT_FORTROLIG_ADRESSE,
            geografiskEnhet = null,
            region = null,
            avvistGrunn = null,
        ),
        Personalia(
            deltaker5Id,
            navn = "Kari Nordmann",
            norskIdent = NorskIdent("01010199998"),
            oppfolgingEnhet = null,
            gradering = Gradering.STRENGT_FORTROLIG_UTLAND,
            geografiskEnhet = null,
            region = null,
            avvistGrunn = null,
        ),
    )

    val linjer = listOf(
        ArrangforflateUtbetalingLinje(
            id = UUID.randomUUID(),
            tilsagn = ArrangorflateTilsagnSummary(
                id = UUID.randomUUID(),
                bestillingsnummer = "A-1-1",
            ),
            status = UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
            pris = 99.NOK,
            statusSistOppdatert = LocalDate.of(2025, 1, 3).atStartOfDay(),
        ),
        ArrangforflateUtbetalingLinje(
            id = UUID.randomUUID(),
            tilsagn = ArrangorflateTilsagnSummary(
                id = UUID.randomUUID(),
                bestillingsnummer = "A-1-2",
            ),
            status = UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING,
            pris = 1.NOK,
            statusSistOppdatert = LocalDate.of(2025, 1, 3).atStartOfDay(),
        ),
    )

    context("pdf-content for utbetalingsdetaljer til arrangør") {
        test("fast sats per tiltaksplass per maned") {
            val pdfContent = UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(
                utbetalingFastSats,
                linjer,
                gjennomforing,
            )

            expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent)).toMatchDisk("utbetalingsdetaljerFastSats")
        }

        test("avtalt pris per time oppfølging per deltaker") {
            val pdfContent = UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(
                utbetalingPrisPerTimeOppfolging,
                linjer,
                gjennomforing,
            )

            expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent)).toMatchDisk("utbetalingsdetaljerTimesPris")
        }
    }

    context("pdf-content for journalpost av innsending fra arrangør") {
        test("fast sats per tiltaksplass per maned") {
            val pdfContent = UbetalingToPdfDocumentContentMapper.toJournalpostPdfContent(
                utbetalingFastSats,
                personalia,
                gjennomforing,
            )

            expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent)).toMatchDisk("journalpostFastSats")
        }
        test("avtalt pris per time oppfølging per deltaker") {
            val pdfContent = UbetalingToPdfDocumentContentMapper.toJournalpostPdfContent(
                utbetalingPrisPerTimeOppfolging,
                personalia,
                gjennomforing,
            )

            expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent)).toMatchDisk("journalpostTimesPris")
        }
    }
})
