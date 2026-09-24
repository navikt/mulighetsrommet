package no.nav.mulighetsrommet.api.tilskuddbehandling

import arrow.core.Either
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.helved.HelVedSimuleringClient
import no.nav.mulighetsrommet.api.clients.helved.HelVedSimuleringResponse
import no.nav.mulighetsrommet.api.clients.helved.HelVedSimuleringsError
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.tilskuddbehandling.kafka.toHelVedUtbetaling
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService.OnBehalfOf
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class TilskuddService(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val helVedSimuleringClient: HelVedSimuleringClient,
) {

    suspend fun simulerOpphor(
        gjennomforingId: UUID,
        tilskuddVedtakId: UUID,
        accessType: AccessType,
    ): Either<HelVedSimuleringsError, HelVedSimuleringResponse> = helVedSimuleringClient.simuler(
        hentHelVedUtbetaling(gjennomforingId, tilskuddVedtakId),
        accessType,
    )

    suspend fun hentHelVedUtbetaling(gjennomforingId: UUID, tilskuddVedtakId: UUID): HelVedUtbetaling {
        val (brukerUtbetaling, deltakerId) = db.session {
            val brukerUtbetaling = queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId)
                ?: error("Fant ikke bruker_utbetaling for tilskuddVedtakId=$tilskuddVedtakId")
            val deltaker = repository.deltaker.getByGjennomforing(gjennomforingId)
                .singleOrNull()
                ?: error("Fant ikke deltaker for tilskuddVedtakId=$tilskuddVedtakId")
            brukerUtbetaling to deltaker.id
        }
        val personalia = personaliaService.getPersonalia(deltakerId, OnBehalfOf.System)

        return brukerUtbetaling
            // Ny behandlingId
            .copy(behandlingId = brukerUtbetaling.behandlingId.plus(1), belop = 0)
            .toHelVedUtbetaling(personalia.norskIdent())
    }
}
