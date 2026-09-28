package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.arrangor.KontoregisterError
import no.nav.mulighetsrommet.admin.arrangor.KontoregisterGateway
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDateTime
import java.util.UUID

/**
 * Samler informasjonen som trengs for å produsere innholdet i et tilsagnsbrev for en enkeltplass
 */
data class TilsagnsbrevInnhold(
    val tiltaksnummer: Tiltaksnummer,
    val tilsagn: Tilsagn,
    val personalia: Personalia,
    val arrangor: Arrangor,
    val kontonummer: Kontonummer?,
    val saksbehandler: String,
    val beslutter: String,
    val besluttetTidspunkt: LocalDateTime,
)

suspend fun QueryContext.hentTilsagnsbrevInnhold(
    tilsagnId: UUID,
    personaliaService: PersonaliaService,
    kontoregister: KontoregisterGateway,
): Either<String, TilsagnsbrevInnhold> {
    val tilsagn = queries.tilsagn.getOrError(tilsagnId)

    val enkeltplass = queries.gjennomforing.getGjennomforingEnkeltplassOrError(tilsagn.gjennomforing.id)
    val deltakere = repository.deltaker.getByGjennomforing(enkeltplass.id)
    val deltaker = when (deltakere.size) {
        1 -> deltakere.single()
        0 -> return "Fant ingen deltaker for enkeltplass ${enkeltplass.id}".left()
        else -> return "Fant ${deltakere.size} deltakere for enkeltplass ${enkeltplass.id}".left()
    }
    val personalia = personaliaService.getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.System)
    val arrangor = repository.arrangor.get(tilsagn.arrangor.id)

    val kontonummer = kontoregister.hentKontonummer(arrangor.organisasjonsnummer).getOrElse {
        when (it) {
            KontoregisterError.IkkeFunnet -> null
            KontoregisterError.Feil -> return "Kunne ikke hente kontonummer for arrangør ${arrangor.organisasjonsnummer.value}: $it".left()
        }
    }

    val opprettelse = queries.totrinnskontroll.getOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
    val saksbehandler = (opprettelse.behandling.utfortAv as? NavIdent)?.let { queries.ansatt.get(it) }
        ?: return "Klarte ikke utlede saksbehandler fra totrinnskontroll id=${opprettelse.id}".left()

    val beslutning = opprettelse.beslutning ?: return "Tilsagn $tilsagnId er ikke besluttet".left()
    val beslutter = (beslutning.utfortAv as? NavIdent)?.let { queries.ansatt.get(it) }
        ?: return "Klarte ikke utlede beslutter fra totrinnskontroll id=${opprettelse.id}".left()

    return TilsagnsbrevInnhold(
        tiltaksnummer = enkeltplass.lopenummer,
        tilsagn = tilsagn,
        personalia = personalia,
        arrangor = arrangor,
        kontonummer = kontonummer,
        saksbehandler = saksbehandler.fulltNavn(),
        beslutter = beslutter.fulltNavn(),
        besluttetTidspunkt = beslutning.tidspunkt.tilNorskLocalDateTime(),
    ).right()
}
