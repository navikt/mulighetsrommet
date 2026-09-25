package no.nav.mulighetsrommet.api.tilskuddbehandling.task

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDateTime
import java.util.UUID

/**
 * Samler informasjonen som trengs for å produsere innholdet i et vedtaksbrev for tilskudd
 */
data class VedtaksbrevInnhold(
    val tilskuddvedtak: List<TilskuddBrevVedtak>,
    val arrangorNavn: String,
    val deltakerPersonalia: DeltakerPersonalia,
    val besluttetTidspunkt: LocalDateTime,
    val tiltak: Tiltak,
    val saksbehandler: String,
    val beslutter: String,
    val behandlendeEnhet: String,
)

data class Arrangor(
    val navn: String,
    val organisasjonsnummer: Organisasjonsnummer,
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
    val utbetalingMottaker: TilskuddMottaker,
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
    behandlingId: UUID,
    personaliaService: PersonaliaService,
): Either<String, VedtaksbrevInnhold> {
    val tilskuddBehandling = queries.tilskuddBehandling.getOrError(behandlingId)
    val gjennomforing = queries.gjennomforing.getGjennomforingEnkeltplassOrError(tilskuddBehandling.gjennomforingId)
    val deltaker = repository.deltaker.getByGjennomforing(gjennomforing.id).single()
    val personalia = personaliaService.getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.System)
    val behandlendeEnhet = queries.enhet.get(tilskuddBehandling.behandlendeEnhet)?.navn
        ?: return "Fant ikke behandlende enhet for behadnling $behandlingId".left()

    val periode = validateGjennomforingPeriode(gjennomforing)
        .fold({ return it.left() }, { it })

    val deltakerPersonalia = validateDeltakerPersonalia(personalia)
        .fold({ return it.left() }, { it })

    val tiltak = Tiltak(
        navn = gjennomforing.navn,
        type = gjennomforing.tiltakstype.tiltakskode.name,
        lopenummer = gjennomforing.lopenummer.value,
        periode = periode,
    )

    val tilskuddvedtak = tilskuddBehandling.tilskudd.map { tilskudd ->
        TilskuddBrevVedtak(
            periode = tilskudd.periode,
            tilskuddType = tilskudd.tilskuddOpplaeringType.toDisplayName(),
            vedtakResultat = tilskudd.vedtakResultat.type,
            begrunnelse = tilskudd.kommentarVedtaksbrev,
            belop = tilskudd.utbetalingBelop?.belop?.let {
                Belop(
                    belop = it,
                    valuta = tilskudd.utbetalingBelop.valuta.name,
                )
            },
            utbetalingMottaker = tilskudd.utbetalingMottaker,
        )
    }

    val opprettelse = queries.totrinnskontroll.getOrError(behandlingId, TotrinnskontrollType.TILSKUDD_OPPRETTELSE)
    val saksbehandler = (opprettelse.behandling.utfortAv as? NavIdent)?.let { queries.ansatt.get(it) }
        ?: return "Klarte ikke utlede saksbehandler fra totrinnskontroll".left()

    val beslutning = opprettelse.beslutning ?: return "Tilskuddsbehandling $behandlingId er ikke besluttet".left()
    val beslutter = (beslutning.utfortAv as? NavIdent)?.let { queries.ansatt.get(it) }
        ?: return "Klarte ikke utlede beslutter fra totrinnskontroll".left()

    return VedtaksbrevInnhold(
        tilskuddvedtak = tilskuddvedtak,
        tiltak = tiltak,
        deltakerPersonalia = deltakerPersonalia,
        arrangorNavn = gjennomforing.arrangor.navn,
        saksbehandler = saksbehandler.fulltNavn(),
        beslutter = beslutter.fulltNavn(),
        behandlendeEnhet = behandlendeEnhet,
        besluttetTidspunkt = beslutning.tidspunkt.tilNorskLocalDateTime(),
    ).right()
}

fun hentForhandsvisningVedtaksbrevInnhold(
    tilskuddBehandling: TilskuddBehandling,
    gjennomforing: Gjennomforing,
): Either<String, VedtaksbrevInnhold> {
    val deltakerPersonalia = DeltakerPersonalia(
        navn = "<deltaker-navn>",
        norskIdent = "<deltaker-fnr>",
    )

    val tiltak = Tiltak(
        navn = gjennomforing.navn,
        type = gjennomforing.tiltakstype.tiltakskode.name,
        lopenummer = gjennomforing.lopenummer.value,
        periode = validateGjennomforingPeriode(gjennomforing)
            .fold({ return it.left() }, { it }),
    )

    val tilskuddvedtak = tilskuddBehandling.tilskudd.map { tilskudd ->
        TilskuddBrevVedtak(
            periode = tilskudd.periode,
            tilskuddType = tilskudd.tilskuddOpplaeringType.toDisplayName(),
            vedtakResultat = tilskudd.vedtakResultat,
            begrunnelse = tilskudd.kommentarVedtaksbrev,
            belop = tilskudd.utbetalingBelop?.belop?.let {
                Belop(
                    belop = it,
                    valuta = tilskudd.utbetalingBelop.valuta.name,
                )
            },
            utbetalingMottaker = tilskudd.utbetalingMottaker,
        )
    }

    return VedtaksbrevInnhold(
        tilskuddvedtak = tilskuddvedtak,
        tiltak = tiltak,
        deltakerPersonalia = deltakerPersonalia,
        arrangorNavn = gjennomforing.arrangor.navn,
        saksbehandler = "<saksbehandler-navn>",
        beslutter = "<beslutter-navn>",
        behandlendeEnhet = "<enhet-navn>",
        besluttetTidspunkt = LocalDateTime.now(),
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

private fun Opplaeringtilskudd.Kode.toDisplayName(): String = when (this) {
    Opplaeringtilskudd.Kode.SKOLEPENGER -> "skolepenger"
    Opplaeringtilskudd.Kode.STUDIEREISE -> "studiereise"
    Opplaeringtilskudd.Kode.EKSAMENSGEBYR -> "eksamensgebyr"
    Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> "semesteravgift"
    Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> "integrert botilbud"
}
