package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.pdfgen.VedtaksbrevContent
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import java.time.LocalDate

object TilskuddVedtakToVedtaksbrevContent {
    fun toVedtakPdfContent(
        tilskuddBehandling: TilskuddBehandlingDto,
        totrinnskontroll: TotrinnskontrollDto.Besluttet,
        personalia: Personalia,
        gjennomforing: GjennomforingEnkeltplass,
    ): VedtaksbrevContent {
        val navn = splitNavn(personalia.navn())
        val ident = personalia.norskIdent()?.value.orEmpty()
        val vedtakListe = tilskuddBehandling.tilskudd.map { tilskudd ->
            VedtaksbrevContent.Vedtak(
                utfall = tilskudd.vedtakResultat.type.beskrivelse,
                tilskuddType = tilskudd.tilskuddOpplaeringType.toDisplayName(),
                tilskuddBelop = tilskudd.utbetalingBelop?.belop ?: 0,
                valuta = tilskudd.utbetalingBelop?.valuta?.name ?: tilskudd.soknadBelop.valuta.name,
                periode = VedtaksbrevContent.Vedtak.Periode(
                    fradato = tilskuddBehandling.periode.start.toString(),
                    tildato = tilskuddBehandling.periode.getLastInclusiveDate().toString(),
                ),
                kommentar = tilskudd.kommentarVedtaksbrev.orEmpty(),
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
            saksbehandler = totrinnskontroll.behandletAv.navn,
            beslutter = totrinnskontroll.besluttetAv.navn,
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
