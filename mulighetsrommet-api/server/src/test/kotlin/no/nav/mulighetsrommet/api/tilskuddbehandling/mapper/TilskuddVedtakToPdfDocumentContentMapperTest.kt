package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.admin.totrinnskontroll.AgentDto
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilskuddVedtakToPdfDocumentContentMapperTest : FunSpec({
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    val gjennomforing = GjennomforingEnkeltplass(
        id = UUID.fromString("cdc50d11-7d86-4a4b-a8d0-1f8a1be575d0"),
        lopenummer = Tiltaksnummer("2026/9999"),
        tiltakstype = Gjennomforing.Tiltakstype(
            id = UUID.fromString("4d4938fa-d4ad-4697-9e20-0e776f7b0f2f"),
            navn = "Enkeltplass Arbeidsmarkedsopplæring",
            tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        ),
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = UUID.fromString("72c45b92-4452-4b44-b1cd-9cfe7be86222"),
            organisasjonsnummer = Organisasjonsnummer("310438707"),
            navn = "AKSEPTABEL EMPIRISK TIGER AS",
            slettet = false,
        ),
        arena = null,
        navn = "Truckførerkurs",
        status = GjennomforingStatusType.GJENNOMFORES,
        startDato = LocalDate.of(2026, 8, 1),
        sluttDato = LocalDate.of(2027, 6, 30),
        deltidsprosent = 100.0,
        antallPlasser = 1,
        opprettetTidspunkt = Instant.parse("2026-05-01T00:00:00Z"),
        oppdatertTidspunkt = Instant.parse("2026-05-01T00:00:00Z"),
        prismodell = Prismodell.AnnenAvtaltPris(
            id = UUID.fromString("b8f1c0de-0000-4000-8000-000000000001"),
            valuta = Valuta.NOK,
            tilsagnPerDeltaker = false,
            prisbetingelser = null,
            totalbelop = null,
        ),
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        ansvarligEnhet = GjennomforingEnkeltplass.AnsvarligEnhet(
            enhetsnummer = NavEnhetNummer("0387"),
            navn = "Nav Øst-Viken",
        ),
    )

    val periode = Periode(LocalDate.of(2026, 8, 1), LocalDate.of(2027, 7, 1))

    fun tilskuddBehandling(vararg tilskudd: TilskuddDbo) = TilskuddBehandling(
        id = UUID.fromString("a1a1a1a1-0000-4000-8000-000000000001"),
        gjennomforingId = gjennomforing.id,
        soknadJournalpostId = "J-2026-001",
        soknadDato = LocalDate.of(2026, 5, 1),
        periode = periode,
        kostnadssted = NavEnhetNummer("0387"),
        tilskudd = tilskudd.toList(),
        status = TilskuddBehandlingStatus.TIL_ATTESTERING,
        kommentarIntern = null,
    )

    val skolepengerInnvilgelse = TilskuddDbo(
        id = UUID.fromString("b2b2b2b2-0000-4000-8000-000000000001"),
        tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
        soknadBelop = ValutaBelop(belop = 50000, valuta = Valuta.NOK),
        utbetalingBelop = ValutaBelop(belop = 50000, valuta = Valuta.NOK),
        vedtakResultat = VedtakResultat.INNVILGELSE,
        kommentarVedtaksbrev = null,
        utbetalingMottaker = TilskuddMottaker.BRUKER,
        kid = Kid.parse("116"),
    )

    val eksamensgebyrAvslag = TilskuddDbo(
        id = UUID.fromString("b2b2b2b2-0000-4000-8000-000000000002"),
        tilskuddOpplaeringType = Opplaeringtilskudd.Kode.EKSAMENSGEBYR,
        soknadBelop = ValutaBelop(belop = 1200, valuta = Valuta.NOK),
        utbetalingBelop = null,
        vedtakResultat = VedtakResultat.AVSLAG,
        kommentarVedtaksbrev = "Søknaden er avslått fordi det ikke er dokumentert at vilkårene for tilskuddet er oppfylt.",
        utbetalingMottaker = TilskuddMottaker.BRUKER,
        kid = null,
    )

    val besluttetTidspunkt = LocalDateTime.of(2026, 5, 26, 12, 0)

    context("pdf-content for vedtaksbrev om tilskudd til opplæring") {
        test("innvilgelse og avslag med to underskrifter") {
            val pdfContent = TilskuddVedtakToPdfDocumentContentMapper.toPdfDocumentContent(
                tilskuddBehandling = tilskuddBehandling(skolepengerInnvilgelse, eksamensgebyrAvslag),
                navn = "Ola Nordmann",
                norskIdent = NorskIdent("01010112345"),
                gjennomforing = gjennomforing,
                saksbehandler = AgentDto.fromAgent(NavIdent("Z123456"), "Sara Saksbehandler"),
                beslutter = AgentDto.fromAgent(NavIdent("Z654321"), "Bertil Beslutter"),
                besluttetTidspunkt = besluttetTidspunkt,
            )

            expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent))
                .toMatchDisk("vedtakInnvilgelseOgAvslag")
        }

        test("automatisk brev uten saksbehandler og beslutter") {
            val pdfContent = TilskuddVedtakToPdfDocumentContentMapper.toPdfDocumentContent(
                tilskuddBehandling = tilskuddBehandling(skolepengerInnvilgelse),
                navn = "Ola Nordmann",
                norskIdent = NorskIdent("01010112345"),
                gjennomforing = gjennomforing,
                saksbehandler = AgentDto.fromAgent(Tiltaksadministrasjon, null),
                beslutter = AgentDto.fromAgent(Tiltaksadministrasjon, null),
                besluttetTidspunkt = besluttetTidspunkt,
            )

            expectSelfie(jsonPrettyPrint.encodeToString<PdfDocumentContent>(pdfContent))
                .toMatchDisk("vedtakAutomatisk")
        }
    }
})
