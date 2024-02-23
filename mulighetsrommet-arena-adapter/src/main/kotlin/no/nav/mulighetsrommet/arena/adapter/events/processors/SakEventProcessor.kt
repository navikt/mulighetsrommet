package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.raise.either
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaSak
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService

class SakEventProcessor(
    private val entities: ArenaEntityService,
) : ArenaEventProcessor {

    override val arenaTable: ArenaTable = ArenaTable.Sak

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val data = event.decodePayload<ArenaSak>()

        if (!sakIsRelatedToTiltaksgjennomforing(data)) {
            return@either ProcessingResult(
                Ignored,
                """Sak ignorert fordi den ikke er en tiltakssak (SAKSKODE != "TILT")""",
            )
        }

        if (!sakHasEnhet(data)) {
            return@either ProcessingResult(
                Ignored,
                "Sak ignorert fordi den ikke har en tilhørende enhet (AETATENHET_ANSVARLIG = null)",
            )
        }

        data
            .toSak()
            .flatMap { entities.upsertSak(it) }
            .map { ProcessingResult(Handled) }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        entities.deleteSak(event.arenaId.toInt()).bind()
    }

    override fun getDependentEntities(event: ArenaEvent): List<ArenaEntityMapping> {
        return entities.getTiltaksgjennomforingBySakId(event.arenaId.toInt())
            ?.let {
                entities.getMapping(ArenaTable.Tiltaksgjennomforing, it.tiltaksgjennomforingId.toString()).getOrNull()
            }
            ?.nel()
            ?: listOf()
    }

    private fun sakIsRelatedToTiltaksgjennomforing(payload: ArenaSak): Boolean = payload.SAKSKODE == "TILT"

    private fun sakHasEnhet(payload: ArenaSak): Boolean = payload.AETATENHET_ANSVARLIG.isNotBlank()

    private fun ArenaSak.toSak() = Either
        .catch {
            Sak(
                sakId = SAK_ID,
                aar = AAR,
                lopenummer = LOPENRSAK,
                enhet = AETATENHET_ANSVARLIG,
            )
        }
        .mapLeft { ProcessingError.InvalidPayload(it.localizedMessage) }
}
