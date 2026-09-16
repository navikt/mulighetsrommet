package no.nav.mulighetsrommet.api.tilskuddbehandling.task

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDateTime
import java.util.UUID

/**
 * Samler informasjonen som trengs for å produsere innholdet i et vedtaksbrev for tilskudd
 */
data class VedtaksbrevInnhold(
    val tilskuddvedtak: List<TilskuddBrevVedtak>,
    val arrangor: Arrangor,
    val deltakerPersonalia: DeltakerPersonalia,
    val besluttetTidspunkt: LocalDateTime,
    val tiltak: Tiltak,
    val saksbehandler: String,
    val beslutter: String,
    val enhet: String,
)

data class Arrangor(
    val navn: String,
    val organisasjonsnummer: String,
)

data class DeltakerPersonalia(
    val navn: String,
    val norskIdent: String,
)

data class TilskuddBrevVedtak(
    val tilskuddType: String,
    val vedtakResultat: VedtakResultat,
    val begrunnelse: String?,
    val belop: Belop?,
    val periode: Periode,
)

data class Belop(
    val belop: Int,
    val valuta: String,
)

data class Tiltak(
    val navn: String,
    val type: String,
    val lopenummer: String,
    val periode: Periode,
)

suspend fun QueryContext.hentVedtaksbrevInnhold(
    vedtakId: UUID,
    personaliaService: PersonaliaService,
): Either<String, VedtaksbrevInnhold> {
    val tilskuddBehandling = queries.tilskuddBehandling.getOrError(vedtakId)
    val gjennomforing = queries.gjennomforing.getGjennomforingEnkeltplassOrError(tilskuddBehandling.gjennomforingId)
    val deltaker = repository.deltaker.getByGjennomforing(gjennomforing.id).single()
    val personalia = personaliaService.getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.System)

    val periode = validateGjennomforingPeriode(gjennomforing)
        .fold({ return it.left() }, { it })

    val deltakerPersonalia = validateDeltakerPersonalia(personalia)
        .fold({ return it.left() }, { it })

    val totrinnskontroll = queries.totrinnskontroll.getDtoOrError(
        vedtakId,
        TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
    )
    val (saksbehandlerNavn, beslutterNavn) = validateSignaturNavn(vedtakId, totrinnskontroll)
        .fold({ return it.left() }, { it })

    val arrangor = Arrangor(
        navn = gjennomforing.arrangor.navn,
        organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer.value,
    )

    val tiltak = Tiltak(
        navn = gjennomforing.navn,
        type = gjennomforing.tiltakstype.tiltakskode.name,
        lopenummer = gjennomforing.lopenummer.value,
        periode = periode,
    )

    val tilskuddvedtak = tilskuddBehandling.tilskudd.map { tilskudd ->
        TilskuddBrevVedtak(
            periode = tilskuddBehandling.periode,
            tilskuddType = tilskudd.tilskuddOpplaeringType.toDisplayName(),
            vedtakResultat = tilskudd.vedtakResultat.type,
            begrunnelse = tilskudd.kommentarVedtaksbrev,
            belop = tilskudd.utbetalingBelop?.belop?.let {
                Belop(
                    belop = it,
                    valuta = tilskudd.utbetalingBelop.valuta.name,
                )
            },
        )
    }

    return VedtaksbrevInnhold(
        tilskuddvedtak = tilskuddvedtak,
        tiltak = tiltak,
        deltakerPersonalia = deltakerPersonalia,
        arrangor = arrangor,
        saksbehandler = formatNavn(saksbehandlerNavn),
        beslutter = formatNavn(beslutterNavn),
        enhet = gjennomforing.ansvarligEnhet.navn,
        besluttetTidspunkt = (totrinnskontroll as TotrinnskontrollDto.Besluttet).besluttetTidspunkt,
    ).right()
}

private fun validateGjennomforingPeriode(gjennomforing: Gjennomforing): Either<String, Periode> {
    val startDato = gjennomforing.startDato
        ?: return "Gjennomføring ${gjennomforing.id} mangler startdato".left()
    val sluttDato = gjennomforing.sluttDato
        ?: return "Gjennomføring ${gjennomforing.id} mangler sluttdato".left()
    return Periode(startDato, sluttDato).right()
}

private fun validateDeltakerPersonalia(personalia: Personalia): Either<String, DeltakerPersonalia> {
    if (!personalia.harTilgang()) return "Fikk ikke tilgang til usladdet personalia for ${personalia.deltakerId}".left()

    val norskIdent = personalia.norskIdent()
        ?: return "Fant ikke norsk ident for deltaker ${personalia.deltakerId}".left()

    return DeltakerPersonalia(
        navn = personalia.navn(),
        norskIdent = norskIdent.value,
    ).right()
}

private fun validateSignaturNavn(
    vedtakId: UUID,
    totrinnskontroll: TotrinnskontrollDto,
): Either<String, Pair<String, String>> {
    val besluttet = totrinnskontroll as? TotrinnskontrollDto.Besluttet
        ?: return "Totrinnskontroll for tilskudd $vedtakId er ikke besluttet".left()

    val saksbehandlerNavn = besluttet.behandletAv.navn
        ?: return "Totrinnskontroll for tilskudd $vedtakId mangler saksbehandlernavn".left()
    val beslutterNavn = besluttet.besluttetAv.navn
        ?: return "Totrinnskontroll for tilskudd $vedtakId mangler beslutternavn".left()

    return (saksbehandlerNavn to beslutterNavn).right()
}

private fun formatNavn(fulltNavn: String): String {
    val parts = fulltNavn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return parts.joinToString(" ")
}

private fun Opplaeringtilskudd.Kode.toDisplayName(): String = when (this) {
    Opplaeringtilskudd.Kode.SKOLEPENGER -> "skolepenger"
    Opplaeringtilskudd.Kode.STUDIEREISE -> "studiereise"
    Opplaeringtilskudd.Kode.EKSAMENSGEBYR -> "eksamensgebyr"
    Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> "semesteravgift"
    Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> "integrert botilbud"
}
