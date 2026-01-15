package no.nav.mulighetsrommet.api.utbetaling.mapper

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.start
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateTilsagnSummary
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelseDeltakelsesprosentPerioder
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsePeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.SatsPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningOutputDeltakelse.BeregnetPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.tiltak.okonomi.Tilskuddstype
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID

class UbetalingToPdfDocumentContentMapperTest : FunSpec({
    @OptIn(ExperimentalSerializationApi::class)
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val deltaker1Id = UUID.randomUUID()
    val deltaker2Id = UUID.randomUUID()
    val deltaker3Id = UUID.randomUUID()

    val utbetalingFastSats = Utbetaling(
        id = UUID.randomUUID(),
        status = UtbetalingStatusType.FERDIG_BEHANDLET,
        godkjentAvArrangorTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        utbetalesTidligstTidspunkt = null,
        createdAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        tiltakstype = Utbetaling.Tiltakstype("Avklaring", Tiltakskode.AVKLARING),
        gjennomforing = Utbetaling.Gjennomforing(
            id = UUID.randomUUID(),
            lopenummer = Tiltaksnummer("2025/10000"),
            navn = "Avklaring hos Nav",
            start = LocalDate.of(2025, 1, 1),
            slutt = null,
        ),
        arrangor = Utbetaling.Arrangor(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Nav",
            slettet = false,
        ),
        beregning = UtbetalingBeregningFastSatsPerTiltaksplassPerManed(
            input = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Input(
                satser = setOf(SatsPeriode(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), 34)),
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
                ),
            ),
            output = UtbetalingBeregningFastSatsPerTiltaksplassPerManed.Output(
                belop = 100,
                deltakelser = setOf(
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker1Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 1.0,
                                sats = 1000,
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker2Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 1.0,
                                sats = 1000,
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
                            ),
                        ),
                    ),
                    UtbetalingBeregningOutputDeltakelse(
                        deltakelseId = deltaker3Id,
                        perioder = setOf(
                            BeregnetPeriode(
                                faktor = 0.75,
                                sats = 1000,
                                periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        betalingsinformasjon = Utbetaling.Betalingsinformasjon(
            kontonummer = Kontonummer("12345678901"),
            kid = null,
        ),
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        innsender = Arrangor,
        journalpostId = null,
        beskrivelse = null,
        begrunnelseMindreBetalt = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
        avbruttBegrunnelse = null,
    )

    val utbetalingPrisPerTimeOppfolging = Utbetaling(
        id = UUID.randomUUID(),
        status = UtbetalingStatusType.FERDIG_BEHANDLET,
        godkjentAvArrangorTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        utbetalesTidligstTidspunkt = null,
        createdAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        tiltakstype = Utbetaling.Tiltakstype("Oppfolging", Tiltakskode.OPPFOLGING),
        gjennomforing = Utbetaling.Gjennomforing(
            id = UUID.randomUUID(),
            lopenummer = Tiltaksnummer("2025/10000"),
            navn = "Oppfolging hos Nav",
            start = LocalDate.of(2025, 1, 1),
            slutt = null,
        ),
        arrangor = Utbetaling.Arrangor(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Nav",
            slettet = false,
        ),
        beregning = UtbetalingBeregningPrisPerTimeOppfolging(
            input = UtbetalingBeregningPrisPerTimeOppfolging.Input(
                satser = setOf(SatsPeriode(Periode.forMonthOf(LocalDate.of(2025, 1, 1)), 34)),
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
                ),
                belop = 100,
            ),
            output = UtbetalingBeregningPrisPerTimeOppfolging.Output(
                belop = 100,
            ),
        ),
        betalingsinformasjon = Utbetaling.Betalingsinformasjon(
            kontonummer = Kontonummer("12345678901"),
            kid = null,
        ),
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        innsender = Arrangor,
        journalpostId = null,
        beskrivelse = null,
        begrunnelseMindreBetalt = null,
        avbruttBegrunnelse = null,
        tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    )

    val personalia = mapOf(
        deltaker1Id to DeltakerPersonalia(
            navn = "Ola Skjermet",
            norskIdent = NorskIdent("01010199999"),
            erSkjermet = true,
            deltakerId = deltaker1Id,
            oppfolgingEnhet = null,
            adressebeskyttelse = PdlGradering.UGRADERT,
        ),
        deltaker2Id to DeltakerPersonalia(
            navn = "Ola Nordmann",
            norskIdent = NorskIdent("01010199999"),
            erSkjermet = false,
            deltakerId = deltaker2Id,
            oppfolgingEnhet = null,
            adressebeskyttelse = PdlGradering.UGRADERT,
        ),
        deltaker3Id to DeltakerPersonalia(
            navn = "Kari Nordmann",
            norskIdent = NorskIdent("01010199998"),
            erSkjermet = false,
            deltakerId = deltaker3Id,
            oppfolgingEnhet = null,
            adressebeskyttelse = PdlGradering.UGRADERT,
        ),
    )

    val linjer = listOf(
        ArrangforflateUtbetalingLinje(
            id = UUID.randomUUID(),
            tilsagn = ArrangorflateTilsagnSummary(
                id = UUID.randomUUID(),
                bestillingsnummer = "A-1-1",
            ),
            status = DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
            belop = 99,
            statusSistOppdatert = LocalDate.of(2025, 1, 3).atStartOfDay(),
        ),
        ArrangforflateUtbetalingLinje(
            id = UUID.randomUUID(),
            tilsagn = ArrangorflateTilsagnSummary(
                id = UUID.randomUUID(),
                bestillingsnummer = "A-1-2",
            ),
            status = DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
            belop = 1,
            statusSistOppdatert = LocalDate.of(2025, 1, 3).atStartOfDay(),
        ),
    )

    context("pdf-content for utbetalingsdetaljer til arrangør") {
        test("fast sats per tiltaksplass per maned") {
            val pdfContent =
                UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(utbetalingFastSats, linjer)

            jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent) shouldBe expectedUtbetalingsdetaljerFastSatsContent
        }

        test("avtalt pris per time oppfølging per deltaker") {
            val pdfContent =
                UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(utbetalingPrisPerTimeOppfolging, linjer)

            jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent) shouldBe expectedUtbetalingsdetaljerTimesPrisContent
        }
    }

    context("pdf-content for journalpost av innsending fra arrangør") {
        test("fast sats per tiltaksplass per maned") {
            val pdfContent = UbetalingToPdfDocumentContentMapper.toJournalpostPdfContent(utbetalingFastSats, personalia)

            jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent) shouldBe expectedJournalpostFastSatsContent
        }
        test("avtalt pris per time oppfølging per deltaker") {
            val pdfContent =
                UbetalingToPdfDocumentContentMapper.toJournalpostPdfContent(utbetalingPrisPerTimeOppfolging, personalia)

            jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent) shouldBe expectedJournalpostTimesPrisContent
        }
    }
})

@Language("JSON")
private val expectedUtbetalingsdetaljerFastSatsContent = """
{
  "title": "Utbetalingsdetaljer",
  "subject": "Utbetaling til Nav",
  "description": "Detaljer om utbetaling for gjennomføring av Avklaring",
  "author": "Nav",
  "sections": [
    {
      "title": {
        "text": "Detaljer om utbetaling",
        "level": 1
      }
    },
    {
      "title": {
        "text": "Innsending",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Arrangør",
              "value": "Nav (123456789)"
            },
            {
              "label": "Dato innsendt av arrangør",
              "value": "02.01.2025"
            },
            {
              "label": "Tiltakstype",
              "value": "Avklaring"
            },
            {
              "label": "Løpenummer",
              "value": "2025/10000"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Utbetalingsperiode",
              "value": "01.01.2025 - 31.01.2025"
            },
            {
              "label": "Utbetales tidligst",
              "value": null,
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Sats",
              "value": "34",
              "format": "NOK"
            },
            {
              "label": "Antall månedsverk",
              "value": "0.0"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Beløp",
              "value": "100",
              "format": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Betalingsinformasjon",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Kontonummer",
              "value": "12345678901"
            },
            {
              "label": "KID-nummer",
              "value": null
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetalingsstatus",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "label": "Godkjent beløp til utbetaling",
              "value": "100",
              "format": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Tilsagn som er brukt til utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Tilsagn",
              "value": "A-1-1"
            },
            {
              "label": "Beløp til utbetaling",
              "value": "99",
              "format": "NOK"
            },
            {
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "label": "Status endret",
              "value": "2025-01-03T00:00",
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Tilsagn",
              "value": "A-1-2"
            },
            {
              "label": "Beløp til utbetaling",
              "value": "1",
              "format": "NOK"
            },
            {
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "label": "Status endret",
              "value": "2025-01-03T00:00",
              "format": "DATE"
            }
          ]
        }
      ]
    }
  ]
}
""".trimIndent()

@Language("JSON")
private val expectedUtbetalingsdetaljerTimesPrisContent = """
    {
      "title": "Utbetalingsdetaljer",
      "subject": "Utbetaling til Nav",
      "description": "Detaljer om utbetaling for gjennomføring av Oppfolging",
      "author": "Nav",
      "sections": [
        {
          "title": {
            "text": "Detaljer om utbetaling",
            "level": 1
          }
        },
        {
          "title": {
            "text": "Innsending",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Arrangør",
                  "value": "Nav (123456789)"
                },
                {
                  "label": "Dato innsendt av arrangør",
                  "value": "02.01.2025"
                },
                {
                  "label": "Tiltakstype",
                  "value": "Oppfolging"
                },
                {
                  "label": "Løpenummer",
                  "value": "2025/10000"
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Utbetaling",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Utbetalingsperiode",
                  "value": "01.01.2025 - 31.01.2025"
                },
                {
                  "label": "Utbetales tidligst",
                  "value": null,
                  "format": "DATE"
                }
              ]
            },
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Avtalt pris per time oppfølging",
                  "value": "34",
                  "format": "NOK"
                }
              ]
            },
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Beløp",
                  "value": "100",
                  "format": "NOK"
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Betalingsinformasjon",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Kontonummer",
                  "value": "12345678901"
                },
                {
                  "label": "KID-nummer",
                  "value": null
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Utbetalingsstatus",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Status",
                  "value": "Overført til utbetaling",
                  "format": "STATUS_SUCCESS"
                },
                {
                  "label": "Godkjent beløp til utbetaling",
                  "value": "100",
                  "format": "NOK"
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Tilsagn som er brukt til utbetaling",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Tilsagn",
                  "value": "A-1-1"
                },
                {
                  "label": "Beløp til utbetaling",
                  "value": "99",
                  "format": "NOK"
                },
                {
                  "label": "Status",
                  "value": "Overført til utbetaling",
                  "format": "STATUS_SUCCESS"
                },
                {
                  "label": "Status endret",
                  "value": "2025-01-03T00:00",
                  "format": "DATE"
                }
              ]
            },
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Tilsagn",
                  "value": "A-1-2"
                },
                {
                  "label": "Beløp til utbetaling",
                  "value": "1",
                  "format": "NOK"
                },
                {
                  "label": "Status",
                  "value": "Overført til utbetaling",
                  "format": "STATUS_SUCCESS"
                },
                {
                  "label": "Status endret",
                  "value": "2025-01-03T00:00",
                  "format": "DATE"
                }
              ]
            }
          ]
        }
      ]
    }
""".trimIndent()

@Language("JSON")
private val expectedJournalpostFastSatsContent = """
{
  "title": "Utbetaling",
  "subject": "Krav om utbetaling fra Nav",
  "description": "Krav om utbetaling fra Nav",
  "author": "Tiltaksadministrasjon",
  "sections": [
    {
      "title": {
        "text": "Innsendt krav om utbetaling",
        "level": 1
      }
    },
    {
      "title": {
        "text": "Innsending",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Arrangør",
              "value": "Nav (123456789)"
            },
            {
              "label": "Dato innsendt av arrangør",
              "value": "02.01.2025"
            },
            {
              "label": "Tiltakstype",
              "value": "Avklaring"
            },
            {
              "label": "Løpenummer",
              "value": "2025/10000"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Utbetalingsperiode",
              "value": "01.01.2025 - 31.01.2025"
            },
            {
              "label": "Utbetales tidligst",
              "value": null,
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Sats",
              "value": "34",
              "format": "NOK"
            },
            {
              "label": "Antall månedsverk",
              "value": "0.0"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Beløp",
              "value": "100",
              "format": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Betalingsinformasjon",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "label": "Kontonummer",
              "value": "12345678901"
            },
            {
              "label": "KID-nummer",
              "value": null
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Stengt hos arrangør",
        "level": 2
      },
      "blocks": [
        {
          "type": "item-list",
          "description": "Det er registrert stengt hos arrangør i følgende perioder:",
          "items": [
            "07.01.2025 - 13.01.2025: Stengt for ferie"
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Deltakerperioder",
        "level": 2
      },
      "blocks": [
        {
          "type": "table",
          "table": {
            "columns": [
              {
                "title": "Navn"
              },
              {
                "title": "Fødselsnr.",
                "align": "RIGHT"
              },
              {
                "title": "Startdato i perioden",
                "align": "RIGHT"
              },
              {
                "title": "Sluttdato i perioden",
                "align": "RIGHT"
              },
              {
                "title": "Deltakelsesprosent",
                "align": "RIGHT"
              }
            ],
            "rows": [
              {
                "cells": [
                  {
                    "value": "Skjermet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01010199999"
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Kari Nordmann"
                  },
                  {
                    "value": "01010199998"
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "14.01.2025"
                  },
                  {
                    "value": "50.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Kari Nordmann"
                  },
                  {
                    "value": "01010199998"
                  },
                  {
                    "value": "15.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              }
            ]
          }
        }
      ]
    },
    {
      "title": {
        "text": "Beregnet månedsverk",
        "level": 2
      },
      "blocks": [
        {
          "type": "table",
          "table": {
            "columns": [
              {
                "title": "Navn"
              },
              {
                "title": "Fødselsnr.",
                "align": "RIGHT"
              },
              {
                "title": "Månedsverk",
                "align": "RIGHT"
              }
            ],
            "rows": [
              {
                "cells": [
                  {
                    "value": "Skjermet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "1.0"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01010199999"
                  },
                  {
                    "value": "1.0"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Kari Nordmann"
                  },
                  {
                    "value": "01010199998"
                  },
                  {
                    "value": "0.75"
                  }
                ]
              }
            ]
          }
        }
      ]
    }
  ]
}
""".trimIndent()

@Language("JSON")
private val expectedJournalpostTimesPrisContent = """
    {
      "title": "Utbetaling",
      "subject": "Krav om utbetaling fra Nav",
      "description": "Krav om utbetaling fra Nav",
      "author": "Tiltaksadministrasjon",
      "sections": [
        {
          "title": {
            "text": "Innsendt krav om utbetaling",
            "level": 1
          }
        },
        {
          "title": {
            "text": "Innsending",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Arrangør",
                  "value": "Nav (123456789)"
                },
                {
                  "label": "Dato innsendt av arrangør",
                  "value": "02.01.2025"
                },
                {
                  "label": "Tiltakstype",
                  "value": "Oppfolging"
                },
                {
                  "label": "Løpenummer",
                  "value": "2025/10000"
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Utbetaling",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Utbetalingsperiode",
                  "value": "01.01.2025 - 31.01.2025"
                },
                {
                  "label": "Utbetales tidligst",
                  "value": null,
                  "format": "DATE"
                }
              ]
            },
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Avtalt pris per time oppfølging",
                  "value": "34",
                  "format": "NOK"
                }
              ]
            },
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Beløp",
                  "value": "100",
                  "format": "NOK"
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Betalingsinformasjon",
            "level": 2
          },
          "blocks": [
            {
              "type": "description-list",
              "entries": [
                {
                  "label": "Kontonummer",
                  "value": "12345678901"
                },
                {
                  "label": "KID-nummer",
                  "value": null
                }
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Stengt hos arrangør",
            "level": 2
          },
          "blocks": [
            {
              "type": "item-list",
              "description": "Det er registrert stengt hos arrangør i følgende perioder:",
              "items": [
                "07.01.2025 - 13.01.2025: Stengt for ferie"
              ]
            }
          ]
        },
        {
          "title": {
            "text": "Deltakerperioder",
            "level": 2
          },
          "blocks": [
            {
              "type": "table",
              "table": {
                "columns": [
                  {
                    "title": "Navn"
                  },
                  {
                    "title": "Fødselsnr.",
                    "align": "RIGHT"
                  },
                  {
                    "title": "Startdato i perioden",
                    "align": "RIGHT"
                  },
                  {
                    "title": "Sluttdato i perioden",
                    "align": "RIGHT"
                  }
                ],
                "rows": [
                  {
                    "cells": [
                      {
                        "value": "Skjermet"
                      },
                      {
                        "value": null
                      },
                      {
                        "value": "01.01.2025"
                      },
                      {
                        "value": "31.01.2025"
                      }
                    ]
                  },
                  {
                    "cells": [
                      {
                        "value": "Ola Nordmann"
                      },
                      {
                        "value": "01010199999"
                      },
                      {
                        "value": "01.01.2025"
                      },
                      {
                        "value": "31.01.2025"
                      }
                    ]
                  },
                  {
                    "cells": [
                      {
                        "value": "Kari Nordmann"
                      },
                      {
                        "value": "01010199998"
                      },
                      {
                        "value": "01.01.2025"
                      },
                      {
                        "value": "31.01.2025"
                      }
                    ]
                  }
                ]
              }
            }
          ]
        }
      ]
    }
""".trimIndent()
