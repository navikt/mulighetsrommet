package no.nav.mulighetsrommet.api.tilsagn.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.admin.navenhet.toDto
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningAnnenAvtaltPris
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.tilsagn.task.TilsagnsbrevInnhold
import no.nav.mulighetsrommet.api.utbetaling.service.Gradering
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NOK
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.tiltak.okonomi.BestillingStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilsagnToPdfDocumentContentMapperTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val deltaker = Personalia(
        deltakerId = UUID.randomUUID(),
        norskIdent = NorskIdent("01010199999"),
        navn = "Normann, Ola",
        gradering = Gradering.UGRADERT,
        oppfolgingEnhet = NavEnhetFixtures.Sel.toDto(),
        geografiskEnhet = null,
        region = null,
        avvistGrunn = null,
    )

    val skjermetDeltaker = Personalia(
        deltakerId = UUID.randomUUID(),
        norskIdent = NorskIdent("01010199998"),
        navn = "Normann, Olve",
        gradering = Gradering.SKJERMING,
        oppfolgingEnhet = NavEnhetFixtures.Sel.toDto(),
        geografiskEnhet = null,
        region = null,
        avvistGrunn = null,
    )

    val adressebekyttetDeltaker = Personalia(
        deltakerId = UUID.randomUUID(),
        norskIdent = NorskIdent("01010199997"),
        navn = "Normann, Olivia",
        gradering = Gradering.FORTROLIG_ADRESSE,
        oppfolgingEnhet = NavEnhetFixtures.Sel.toDto(),
        geografiskEnhet = null,
        region = null,
        avvistGrunn = null,
    )

    val kontonummer = Kontonummer("12345678910")

    val saksbehandler = "Saksbehandler Navn"
    val beslutter = "Beslutter Navn"

    val arrangor = Arrangor.Norsk.opprett(
        id = UUID.fromString("4d4938fa-d4ad-4697-9e20-0e776f7b0f2f"),
        organisasjonsnummer = Organisasjonsnummer("310438707"),
        navn = "AKSEPTABEL EMPIRISK TIGER AS",
        organisasjonsform = "AS",
    )

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
        belopBrukt = 0.NOK,
        periode = Periode.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))!!,
        lopenummer = 1,
        bestilling = Tilsagn.Bestilling(
            bestillingsnummer = "A-2026/9999-1",
            status = BestillingStatusType.AKTIV,
        ),
        kostnadssted = NavEnhet(
            enhetsnummer = NavEnhetNummer("0387"),
            navn = "Nav tiltak Oslo",
            type = NavEnhetType.TILTAK,
            overordnetEnhet = null,
            status = NavEnhetStatus.AKTIV,
        ),
        arrangor = Tilsagn.Arrangor(
            id = arrangor.id,
            organisasjonsnummer = arrangor.organisasjonsnummer,
            navn = arrangor.navn,
            slettet = false,
        ),
        status = TilsagnStatus.GODKJENT,
        kommentar = null,
        beskrivelse = null,
        tilsagnsbrev = null,
        beregning = TilsagnBeregningAnnenAvtaltPris(
            input = TilsagnBeregningAnnenAvtaltPris.Input(
                listOf(
                    TilsagnBeregningAnnenAvtaltPris.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "1234",
                        pris = 1234.NOK,
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningAnnenAvtaltPris.Output(
                pris = 1234.NOK,
            ),
        ),
        deltakere = emptyList(),
    )

    context("pdf-content for tilsagnsbrev til arrangør") {
        context("når deltaker er adressebeskyttet") {
            val innhold = TilsagnsbrevInnhold(
                tilsagn = tilsagn,
                personalia = adressebekyttetDeltaker,
                arrangor = arrangor,
                kontonummer = kontonummer,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                tiltaksnummer = tilsagn.gjennomforing.lopenummer,
                besluttetTidspunkt = LocalDateTime.of(2026, 3, 1, 10, 0),
            )

            test("inkluderer personalia kun når visningsflagg er satt til true") {
                val medPersonalia = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
                    innhold,
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = true,
                )
                expectSelfie(jsonPrettyPrint.encodeToString(medPersonalia)).toMatchDisk("adressebeskyttet-med-personalia")

                val utenPersonalia = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
                    innhold,
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = false,
                )
                expectSelfie(jsonPrettyPrint.encodeToString(utenPersonalia)).toMatchDisk("adressebeskyttet-uten-personalia")
            }
        }

        context("når deltaker er skjermet") {
            val innhold = TilsagnsbrevInnhold(
                tilsagn = tilsagn,
                personalia = skjermetDeltaker,
                arrangor = arrangor,
                kontonummer = kontonummer,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                tiltaksnummer = tilsagn.gjennomforing.lopenummer,
                besluttetTidspunkt = LocalDateTime.of(2026, 3, 1, 10, 0),
            )

            test("inkluderer personalia kun når visningsflagg er satt til true") {
                val medPersonalia = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
                    innhold,
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = true,
                )
                expectSelfie(jsonPrettyPrint.encodeToString(medPersonalia)).toMatchDisk("skjermet-med-personalia")

                val utenPersonalia = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
                    innhold,
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = false,
                )
                expectSelfie(jsonPrettyPrint.encodeToString(utenPersonalia)).toMatchDisk("skjermet-uten-personalia")
            }
        }

        context("når person er ugradert") {
            val innhold = TilsagnsbrevInnhold(
                tilsagn = tilsagn,
                personalia = deltaker,
                arrangor = arrangor,
                kontonummer = kontonummer,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                tiltaksnummer = tilsagn.gjennomforing.lopenummer,
                besluttetTidspunkt = LocalDateTime.of(2026, 3, 1, 10, 0),
            )

            test("inkluderer personalia uavhengig om visningsflagg er satt til true eller false") {
                val medPersonalia = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
                    innhold,
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = true,
                )
                expectSelfie(jsonPrettyPrint.encodeToString(medPersonalia)).toMatchDisk("ugradert-med-personalia")

                val utenPersonalia = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
                    innhold,
                    visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = false,
                )
                expectSelfie(jsonPrettyPrint.encodeToString(utenPersonalia)).toMatchDisk("ugradert-med-personalia")
            }
        }
    }
})
