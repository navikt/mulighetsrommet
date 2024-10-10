package no.nav.mulighetsrommet.api.services

import arrow.core.getOrElse
import kotliquery.Session
import no.nav.mulighetsrommet.api.clients.altinn.AltinnClient
import no.nav.mulighetsrommet.api.clients.altinn.AltinnRessurs
import no.nav.mulighetsrommet.api.repositories.ArrangorAnsatt
import no.nav.mulighetsrommet.api.repositories.ArrangorAnsattRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import org.threeten.bp.Duration
import java.time.LocalDateTime
import java.util.*

class ArrangorRolleService(
    private val altinnClient: AltinnClient,
    private val arrangorService: ArrangorService,
    private val db: Database,
    private val arrangorAnsattRepository: ArrangorAnsattRepository,
) {
    private val rolleExpiryDuration = Duration.ofDays(1)

    suspend fun getRoller(norskIdent: NorskIdent): List<ArrangorRolle> {
        val roller = arrangorAnsattRepository.getRoller(norskIdent)
        return if (roller.isEmpty() || roller.any { it.expiry.isBefore(LocalDateTime.now()) }) {
            syncRoller(norskIdent)
        } else {
            roller
        }
    }

    private suspend fun syncRoller(norskIdent: NorskIdent): List<ArrangorRolle> {
        return db.transactionSuspend { tx ->
            val ansatt = arrangorAnsattRepository.getAnsatt(norskIdent, tx)
                ?: run {
                    val nyAnsatt = ArrangorAnsatt(
                        id = UUID.randomUUID(),
                        norskIdent = norskIdent,
                    )
                    arrangorAnsattRepository.upsertAnsatt(nyAnsatt, tx)
                    nyAnsatt
                }

            syncRoller(ansatt, tx)
        }
    }

    private suspend fun syncRoller(ansatt: ArrangorAnsatt, tx: Session): List<ArrangorRolle> {
        return altinnClient.hentRoller(ansatt.norskIdent)
            .map { altinnRolle ->
                val arrangor = arrangorService.getOrSyncArrangorFromBrreg(altinnRolle.organisasjonsnummer.value)
                    .getOrElse { throw Exception("feil ved henting av org fra brreg") }

                ArrangorRolle(
                    arrangorId = arrangor.id,
                    expiry = LocalDateTime.now().plusSeconds(rolleExpiryDuration.seconds),
                    rolle = ArrangorRolleType.fromAltinnRessurs(altinnRolle.ressurs),
                )
            }
            .also {
                arrangorAnsattRepository.upsertRoller(ansatt.id, it, tx)
            }
    }
}

enum class ArrangorRolleType {
    TILTAK_ARRANGOR_REFUSJON,
    ;

    companion object {
        fun fromAltinnRessurs(ressurs: AltinnRessurs) =
            when (ressurs) {
                AltinnRessurs.TILTAK_ARRANGOR_REFUSJON -> TILTAK_ARRANGOR_REFUSJON
            }
    }
}

data class ArrangorRolle(
    val arrangorId: UUID,
    val rolle: ArrangorRolleType,
    val expiry: LocalDateTime,
)
