package no.nav.mulighetsrommet.api.utbetaling.mapper

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.arrangorflate.api.*
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.utbetaling.Person
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakelsesprosentPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.StengtPeriode
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.*
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class UbetalingToPdfDocumentContentMapperTest : FunSpec({

    @OptIn(ExperimentalSerializationApi::class)
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val utbetaling = ArrFlateUtbetaling(
        id = UUID.randomUUID(),
        status = ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
        godkjentAvArrangorTidspunkt = LocalDate.of(2025, 1, 2).atStartOfDay(),
        kanViseBeregning = false,
        createdAt = LocalDate.of(2025, 1, 1).atStartOfDay(),
        tiltakstype = Utbetaling.Tiltakstype("Avklaring", Tiltakskode.AVKLARING),
        gjennomforing = Utbetaling.Gjennomforing(UUID.randomUUID(), "Avklaring hos Nav"),
        arrangor = Utbetaling.Arrangor(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Nav",
            slettet = false,
        ),
        beregning = ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder(
            belop = 100,
            digest = "digest",
            deltakelser = listOf(
                ArrFlateBeregningDeltakelse.PrisPerManedsverkMedDeltakelsesmengder(
                    id = UUID.randomUUID(),
                    deltakerStartDato = LocalDate.of(2025, 1, 1),
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
                    faktor = 1.0,
                    perioderMedDeltakelsesmengde = listOf(
                        DeltakelsesprosentPeriode(
                            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
                            deltakelsesprosent = 100.0,
                        ),
                    ),
                    person = Person(
                        navn = "Ola Nordmann",
                        foedselsdato = LocalDate.of(1989, 1, 1),
                        norskIdent = NorskIdent("01010199999"),
                    ),
                ),
                ArrFlateBeregningDeltakelse.PrisPerManedsverkMedDeltakelsesmengder(
                    id = UUID.randomUUID(),
                    deltakerStartDato = LocalDate.of(2024, 1, 1),
                    periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)),
                    faktor = 0.75,
                    perioderMedDeltakelsesmengde = listOf(
                        DeltakelsesprosentPeriode(
                            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 15)),
                            deltakelsesprosent = 50.0,
                        ),
                        DeltakelsesprosentPeriode(
                            periode = Periode(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 1)),
                            deltakelsesprosent = 100.0,
                        ),
                    ),
                    person = Person(
                        navn = "Kari Nordmann",
                        foedselsdato = LocalDate.of(1989, 1, 1),
                        norskIdent = NorskIdent("22010199998"),
                    ),
                ),
            ),
            stengt = listOf(
                StengtPeriode(
                    periode = Periode(LocalDate.of(2025, 1, 7), LocalDate.of(2025, 1, 14)),
                    beskrivelse = "Stengt for ferie",
                ),
            ),
            antallManedsverk = 1.0,
        ),
        betalingsinformasjon = Utbetaling.Betalingsinformasjon(
            kontonummer = Kontonummer("12345678901"),
            kid = null,
        ),
        periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
        type = null,
        linjer = listOf(
            ArrangorUtbetalingLinje(
                id = UUID.randomUUID(),
                tilsagn = ArrangorUtbetalingLinje.Tilsagn(
                    id = UUID.randomUUID(),
                    bestillingsnummer = "A-1-1",
                ),
                status = DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
                belop = 99,
                statusSistOppdatert = LocalDate.of(2025, 1, 3).atStartOfDay(),
            ),
            ArrangorUtbetalingLinje(
                id = UUID.randomUUID(),
                tilsagn = ArrangorUtbetalingLinje.Tilsagn(
                    id = UUID.randomUUID(),
                    bestillingsnummer = "A-1-2",
                ),
                status = DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
                belop = 1,
                statusSistOppdatert = LocalDate.of(2025, 1, 3).atStartOfDay(),
            ),
        ),
    )

    test("pdf-content for utbetalingsdetaljer til arrangør") {
        val pdfContent = UbetalingToPdfDocumentContentMapper.toUtbetalingsdetaljerPdfContent(utbetaling)

        jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent) shouldBe expectedUtbetalingsdetaljerContent
    }

    test("pdf-content for journalpost av innsending fra arrangør") {
        val pdfContent = UbetalingToPdfDocumentContentMapper.toJournalpostPdfContent(utbetaling)

        jsonPrettyPrint.encodeToString(pdfContent) shouldBe expectedJournalpostContent
    }
})

@Language("JSON")
private val expectedUtbetalingsdetaljerContent = """
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
              "label": "Tiltaksnavn",
              "value": "Avklaring hos Nav"
            },
            {
              "label": "Tiltakstype",
              "value": "Avklaring"
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
              "label": "Antall månedsverk",
              "value": "1.0"
            },
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
private val expectedJournalpostContent = """
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
              "label": "Tiltaksnavn",
              "value": "Avklaring hos Nav"
            },
            {
              "label": "Tiltakstype",
              "value": "Avklaring"
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
              "label": "Antall månedsverk",
              "value": "1.0"
            },
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
                "title": "Fødselsdato",
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
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01.01.1989"
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
                    "value": "01.01.1989"
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
                    "value": "01.01.1989"
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
                "title": "Fødselsdato",
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
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01.01.1989"
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
                    "value": "01.01.1989"
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
