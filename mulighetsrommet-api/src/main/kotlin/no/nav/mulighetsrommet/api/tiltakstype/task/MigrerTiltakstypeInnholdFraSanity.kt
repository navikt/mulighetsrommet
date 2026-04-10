package no.nav.mulighetsrommet.api.tiltakstype.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeRedaksjoneltInnholdRequest
import no.nav.mulighetsrommet.model.Regelverklenke
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class MigrerTiltakstypeInnholdFraSanity(
    private val db: ApiDatabase,
    private val sanityService: SanityService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName)
        .executeSuspend { _, _ ->
            migrerInnhold()
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString())
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    private suspend fun migrerInnhold() {
        val sanityTiltakstyper = sanityService.getTiltakstyper()

        val navnTilId = db.session {
            queries.tiltakstype.getAll()
                .filter { it.sanityId != null }
                .associate { it.navn to it.id }
        }

        val sanityIdTilDbId = db.session {
            queries.tiltakstype.getAll()
                .filter { it.sanityId != null }
                .associate { it.sanityId!! to it.id }
        }

        sanityTiltakstyper.forEach { sanity ->
            val dbId = sanityIdTilDbId[UUID.fromString(sanity._id)]
            if (dbId == null) {
                logger.warn("Fant ingen tiltakstype i databasen for sanityId=${sanity._id}, hopper over")
                return@forEach
            }

            val regelverklenker = sanity.regelverkLenker?.mapNotNull { lenke ->
                val url = lenke.regelverkUrl ?: return@mapNotNull null
                Regelverklenke(
                    regelverkUrl = url,
                    regelverkLenkeNavn = lenke.regelverkLenkeNavn,
                    beskrivelse = lenke.beskrivelse,
                )
            } ?: emptyList()

            val kanKombineresMedIds = sanity.kanKombineresMed.mapNotNull { navn ->
                navnTilId[navn].also {
                    if (it == null) logger.warn("Fant ingen tiltakstype med navn '$navn' for kombinasjon")
                }
            }

            val faneinnhold = sanity.faneinnhold?.copy(delMedBruker = sanity.delingMedBruker)

            val request = TiltakstypeRedaksjoneltInnholdRequest(
                beskrivelse = sanity.beskrivelse,
                faneinnhold = faneinnhold,
                regelverklenker = regelverklenker,
                kanKombineresMed = kanKombineresMedIds,
            )

            db.transaction {
                queries.tiltakstype.upsertRedaksjoneltInnhold(dbId, request)
                queries.tiltakstype.setKanKombineresMed(dbId, kanKombineresMedIds)
            }

            logger.info("Migrert innhold for tiltakstype id=$dbId (sanityId=${sanity._id})")
        }

        logger.info("Migrering av tiltakstypeinnhold fra Sanity fullført. Behandlet ${sanityTiltakstyper.size} tiltakstyper.")
    }
}
