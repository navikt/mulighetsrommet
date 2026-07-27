package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.pdfgen.VedtaksbrevContent
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDate

object TilskuddVedtakToVedtaksbrevContent {
    fun toVedtakPdfContent(
        tilskuddBehandling: TilskuddBehandling,
        navn: String,
        norskIdent: NorskIdent?,
        gjennomforing: GjennomforingEnkeltplass,
        saksbehandler: String,
        beslutter: String,
    ): VedtaksbrevContent {
        val navn = navn
        val norskIdent = norskIdent
        val vedtakListe = tilskuddBehandling.tilskudd.map { t ->
            VedtaksbrevContent.Vedtak(
                utfall = t.vedtakResultat.beskrivelse,
                tilskuddType = t.tilskuddOpplaeringType.toDisplayName(),
                tilskuddBelop = t.utbetalingBelop?.belop ?: 0,
                valuta = t.utbetalingBelop?.valuta?.name ?: t.soknadBelop.valuta.name,
                periode = VedtaksbrevContent.Vedtak.Periode(
                    fradato = tilskuddBehandling.periode.start,
                    tildato = tilskuddBehandling.periode.getLastInclusiveDate(),
                ),
                kommentar = t.kommentarVedtaksbrev.orEmpty(),
            )
        }

        return VedtaksbrevContent(
            deltaker = VedtaksbrevContent.Deltaker(
                navn = navn,
                norskIdent = norskIdent,
            ),
            saksnummer = gjennomforing.lopenummer,
            opprettetDato = LocalDate.now(),
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            enhet = gjennomforing.ansvarligEnhet.navn,
            vedtak = vedtakListe,
        )
    }

    private fun Opplaeringtilskudd.Kode.toDisplayName(): String = when (this) {
        Opplaeringtilskudd.Kode.SKOLEPENGER -> "Skolepenger"
        Opplaeringtilskudd.Kode.STUDIEREISE -> "Studiereise"
        Opplaeringtilskudd.Kode.EKSAMENSGEBYR -> "Eksamensgebyr"
        Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> "Semesteravgift"
        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> "Integrert botilbud"
    }

    private data class NavnParts(
        val fornavn: String,
        val mellomnavn: String,
        val etternavn: String,
    )

    private fun splitNavn(fulltNavn: String): NavnParts {
        val parts = fulltNavn.trim().split("\\s+").filter { it.isNotBlank() }
        return when (parts.size) {
            0 -> NavnParts("", "", "")

            1 -> NavnParts(parts[0], "", "")

            2 -> NavnParts(parts[0], "", parts[1])

            else -> NavnParts(
                fornavn = parts.first(),
                mellomnavn = parts.drop(1).dropLast(1).joinToString(" "),
                etternavn = parts.last(),
            )
        }
    }
}
