package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.pdfgen.VedtaksbrevContent
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate

object TilskuddVedtakToVedtaksbrevContent {
    fun toVedtakPdfContent(
        tilskuddBehandling: TilskuddBehandlingDbo,
        personalia: Personalia,
        gjennomforing: GjennomforingEnkeltplass,
        saksbehandler: String,
        beslutter: String,
    ): VedtaksbrevContent {
        val navn = splitNavn(personalia.navn())
        val ident = personalia.norskIdent()?.value.orEmpty()
        val vedtakListe = tilskuddBehandling.tilskudd.map { t ->
            VedtaksbrevContent.Vedtak(
                utfall = t.vedtakResultat.beskrivelse,
                tilskuddType = t.tilskuddOpplaeringType.toDisplayName(),
                tilskuddBelop = t.utbetalingBelop?.belop ?: 0,
                valuta = t.utbetalingBelop?.valuta?.name ?: t.soknadBelop.valuta.name,
                periode = VedtaksbrevContent.Vedtak.Periode(
                    fradato = tilskuddBehandling.periode.start.toString(),
                    tildato = tilskuddBehandling.periode.getLastInclusiveDate().toString(),
                ),
                kommentar = t.kommentarVedtaksbrev.orEmpty(),
            )
        }

        return VedtaksbrevContent(
            deltaker = VedtaksbrevContent.Deltaker(
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
                personident = ident,
            ),
            saksnummer = gjennomforing.lopenummer.value,
            opprettetDato = LocalDate.now().toString(),
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            avsender = gjennomforing.ansvarligEnhet.navn,
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
