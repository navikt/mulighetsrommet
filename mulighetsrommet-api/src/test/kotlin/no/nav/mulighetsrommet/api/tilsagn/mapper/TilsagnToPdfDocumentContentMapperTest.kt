package no.nav.mulighetsrommet.api.tilsagn.mapper

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.BestillingStatusType
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID

class TilsagnToPdfDocumentContentMapperTest : FunSpec({
    @OptIn(ExperimentalSerializationApi::class)
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val deltakelseId = UUID.randomUUID()
    val deltaker = DeltakerPersonalia(
        deltakerId = deltakelseId,
        norskIdent = NorskIdent("01010199999"),
        navn = "Normann, Ola",
        erSkjermet = false,
        adressebeskyttelse = PdlGradering.UGRADERT,
        oppfolgingEnhet = NavEnhetFixtures.Sel.enhetsnummer,
    )

    val kontonummer = Kontonummer("12345678910")

    val tilsagn = Tilsagn(
        id = UUID.fromString("72c45b92-4452-4b44-b1cd-9cfe7be86222"),
        type = TilsagnType.TILSAGN,
        tiltakstype = Tilsagn.Tiltakstype(
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
            navn = "Enkeltplass Arbeidsmarkedsopplæring",
        ),
        gjennomforing = Tilsagn.Gjennomforing(
            id = UUID.fromString("cdc50d11-7d86-4a4b-a8d0-1f8a1be575d0"),
            lopenummer = Tiltaksnummer("2025/11457"),
            navn = "Truckførerkurs",
        ),
        belopBrukt = 0.withValuta(Valuta.NOK),
        periode = Periode.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))!!,
        lopenummer = 1,
        bestilling = Tilsagn.Bestilling(
            bestillingsnummer = "A-2026/9999-1",
            status = BestillingStatusType.AKTIV,
        ),
        kostnadssted = NavEnhetDbo(
            enhetsnummer = NavEnhetNummer("0387"),
            navn = "Nav tiltak Oslo",
            type = Norg2Type.TILTAK,
            overordnetEnhet = null,
            status = NavEnhetStatus.AKTIV,
        ),
        arrangor = Tilsagn.Arrangor(
            id = UUID.fromString("4d4938fa-d4ad-4697-9e20-0e776f7b0f2f"),
            organisasjonsnummer = Organisasjonsnummer("310438707"),
            navn = "AKSEPTABEL EMPIRISK TIGER AS",
            slettet = false,
        ),
        status = TilsagnStatus.GODKJENT,
        kommentar = null,
        beskrivelse = null,
        journalpost = null,
        beregning =
        TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "1234",
                        pris = 1234.withValuta(Valuta.NOK),
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(
                pris = 1234.withValuta(Valuta.NOK),
            ),
        ),

    )

    context("pdf-content for tilsagnsbrev til arrangør") {
        test("annen avtalt pris") {
            val pdfContent = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(tilsagn, kontonummer, deltaker)

            jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent) shouldBe expectedUtbetalingsdetaljerFastSatsContent
        }
    }
})

@Language("JSON")
private val expectedUtbetalingsdetaljerFastSatsContent = """
{
  "title": "Tilsagnsbrev",
  "subject": "Tilsagnsbrev til AKSEPTABEL EMPIRISK TIGER AS",
  "description": "Detaljer om tilsagn for gjennomføring av Enkeltplass Arbeidsmarkedsopplæring",
  "author": "Nav",
  "topSection": {
    "publicExemption": true,
    "addressedTo": "Brev til AKSEPTABEL EMPIRISK TIGER AS",
    "date": "2026-03-02",
    "reference": "Ref. A-2026/9999-1"
  },
  "sections": [
    {
      "title": {
        "text": "Bekreftelse på bestilling",
        "level": 1
      },
      "blocks": [
        {
          "type": "paragraph",
          "words": [
            {
              "text": "Nav og dere har blitt enige om dette:"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tiltaket",
              "value": "Truckførerkurs"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Deltakeren",
              "value": "Normann, Ola (01010199999)"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetalingsperioden",
              "value": "01.01.2026 - 31.01.2026"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Støtten fra Nav",
              "value": "Opptil 1 234 NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Hvordan kan dere få utbetalt pengene?",
        "level": 2
      },
      "blocks": [
        {
          "type": "paragraph",
          "words": [
            {
              "text": "Gå inn på Navs hjemmesider, velg "
            },
            {
              "text": "Samarbeidspartner",
              "format": "BOLD"
            },
            {
              "text": ", "
            },
            {
              "text": "Tiltaksarrangør",
              "format": "BOLD"
            },
            {
              "text": " og "
            },
            {
              "text": "Skjema og søknad",
              "format": "BOLD"
            },
            {
              "text": ". Velg så "
            },
            {
              "text": "Opplæring",
              "format": "BOLD"
            },
            {
              "text": " og "
            },
            {
              "text": "Faktura",
              "format": "BOLD"
            },
            {
              "text": ". Send inn faktura til Nav med førsteside."
            }
          ]
        },
        {
          "type": "paragraph",
          "words": [
            {
              "text": "Vi kan kontrollere om pengene som blir utbetalt blir brukt riktig."
            }
          ]
        },
        {
          "type": "paragraph",
          "words": [
            {
              "text": "Følgende informasjon er registrert hos NAV:"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Bedriftsnummer",
              "value": "310438707"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Kontonummer",
              "value": "12345678910"
            }
          ]
        },
        {
          "type": "paragraph",
          "words": [
            {
              "text": "Hvis kontonummeret er feil, må dere oppdatere det via Navs hjemmeside under "
            },
            {
              "text": "Arbeidsgiver",
              "format": "BOLD"
            },
            {
              "text": " og "
            },
            {
              "text": "Endre kontonummer",
              "format": "BOLD"
            },
            {
              "text": "."
            }
          ]
        }
      ]
    }
  ],
  "regards": {
    "intro": "Hilsen",
    "subject": "Nav Arbeidsmarkedstiltak",
    "others": [
      "Beslutters navn",
      "Saksbehandlers navn"
    ]
  }
}
""".trimIndent()
