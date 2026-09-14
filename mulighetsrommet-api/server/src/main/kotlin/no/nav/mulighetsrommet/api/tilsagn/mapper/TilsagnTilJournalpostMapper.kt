package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.Journalpost
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDateTime
import java.util.UUID

data class TilsagnJournalpostSnapshot(
    val tilsagnId: UUID,
    val deltaker: NorskIdent,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val arrangorNavn: String,
    val fagsakId: String,
    val besluttetTidspunkt: LocalDateTime,
)

object TilsagnTilJournalpostMapper {
    fun tilJournalpost(
        tilsagn: TilsagnJournalpostSnapshot,
        pdf: ByteArray,
    ): Journalpost = Journalpost(
        tittel = "Tilsagnsbrev",
        journalposttype = "UTGAAENDE",
        avsenderMottaker = Journalpost.AvsenderMottaker(
            id = tilsagn.arrangorOrganisasjonsnummer.value,
            idType = "ORGNR",
            navn = tilsagn.arrangorNavn,
        ),
        bruker = Journalpost.Bruker(
            id = tilsagn.deltaker.value,
            idType = "FNR",
        ),
        tema = "TIL", // Tiltak
        kanal = "INGEN_DISTRIBUSJON", // https://confluence.adeo.no/spaces/BOA/pages/316407153/Utsendingskanal
        journalfoerendeEnhet = "9999", // Automatisk journalføring
        eksternReferanseId = tilsagn.tilsagnId.toString(),
        datoMottatt = tilsagn.besluttetTidspunkt.toString(),
        dokumenter = listOf(
            Journalpost.Dokument(
                tittel = "Tilsagnsbrev",
                brevKode = "Tilsagnsbrev_v1",
                dokumentvarianter = listOf(
                    Journalpost.Dokument.Dokumentvariant(
                        "PDFA",
                        pdf,
                        "ARKIV",
                    ),
                ),
            ),
        ),
        sak = Journalpost.Sak(
            sakstype = Journalpost.Sak.Sakstype.FAGSAK,
            fagsakId = tilsagn.fagsakId,
            fagsaksystem = Journalpost.Sak.Fagsaksystem.TILTAKSADMINISTRASJON,
        ),
    )
}
