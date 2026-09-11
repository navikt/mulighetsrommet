package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import no.nav.mulighetsrommet.admin.totrinnskontroll.AgentDto
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.Kontonummer
import java.util.UUID

/**
 * Samler informasjonen som trengs for å produsere innholdet i et tilsagnsbrev for en enkeltplass
 */
data class TilsagnsbrevInnhold(
    val tilsagn: Tilsagn,
    val personalia: Personalia,
    val arrangor: Arrangor,
    val kontonummer: Kontonummer,
    val saksbehandler: AgentDto,
    val beslutter: AgentDto?,
    val fagsakId: String,
)

suspend fun QueryContext.hentTilsagnsbrevInnhold(
    tilsagnId: UUID,
    personaliaService: PersonaliaService,
    kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
): Either<String, TilsagnsbrevInnhold> {
    val tilsagn = queries.tilsagn.getOrError(tilsagnId)

    val enkeltplass = queries.gjennomforing.getGjennomforingEnkeltplassOrError(tilsagn.gjennomforing.id)
    val deltakere = repository.deltaker.getByGjennomforing(enkeltplass.id)
    val deltaker = when (deltakere.size) {
        1 -> deltakere.single()
        0 -> return Either.Left("Fant ingen deltaker for enkeltplass ${enkeltplass.id}")
        else -> return Either.Left("Fant ${deltakere.size} deltakere for enkeltplass ${enkeltplass.id}")
    }
    val personalia = personaliaService.getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.System)
    val arrangor = repository.arrangor.get(tilsagn.arrangor.id)

    val kontonummer = kontoregisterOrganisasjonClient.getKontonummerForOrganisasjon(arrangor.organisasjonsnummer)
        .map { Kontonummer(it.kontonr) }
        .getOrElse {
            return Either.Left("Kunne ikke hente kontonummer for arrangør ${arrangor.organisasjonsnummer.value}: $it")
        }

    val opprettelse = queries.totrinnskontroll.getDtoOrError(tilsagn.id, TotrinnskontrollType.TILSAGN_OPPRETTELSE)
    val saksbehandler = opprettelse.behandletAv
    val beslutter = when (opprettelse) {
        is TotrinnskontrollDto.Besluttet -> opprettelse.besluttetAv
        is TotrinnskontrollDto.TilBeslutning -> null
    }

    return TilsagnsbrevInnhold(
        tilsagn = tilsagn,
        personalia = personalia,
        arrangor = arrangor,
        kontonummer = kontonummer,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        fagsakId = enkeltplass.arena?.tiltaksnummer?.value ?: enkeltplass.lopenummer.value,
    ).right()
}
