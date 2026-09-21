package no.nav.mulighetsrommet.admin.navansatt.service

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import org.slf4j.LoggerFactory
import java.time.LocalDate

class NavAnsattSyncService(
    private val db: AdminDatabase,
    private val navAnsattService: NavAnsattService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun synchronizeNavAnsatte(today: LocalDate, deletionDate: LocalDate): Unit = db.suspendSession {
        val ansatteToUpsert = navAnsattService.getNavAnsatteForAllRoles()

        logger.info("Oppdaterer ${ansatteToUpsert.size} NavAnsatt fra Azure")
        ansatteToUpsert.forEach { ansatt ->
            val current = repository.navAnsatt.get(ansatt.navIdent)
            if (ansatt != current) {
                repository.navAnsatt.save(ansatt)
            }
        }

        val ansatteEntraObjectIds = ansatteToUpsert.map { it.entraObjectId }
        val ansatteToScheduleForDeletion = repository.navAnsatt.getAll().filter { ansatt ->
            ansatt.skalSlettesDato == null && ansatt.entraObjectId !in ansatteEntraObjectIds
        }
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting oid=${ansatt.entraObjectId} dato=$deletionDate")
            repository.navAnsatt.save(ansatt.skalSlettes(deletionDate))
        }

        val ansatteToDelete = repository.navAnsatt.getAll().filter { ansatt ->
            val skalSlettesDato = ansatt.skalSlettesDato
            skalSlettesDato != null && skalSlettesDato <= today
        }
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting oid=${ansatt.entraObjectId} dato=${ansatt.skalSlettesDato}")
            deleteNavAnsatt(ansatt)
        }
    }

    private fun deleteNavAnsatt(ansatt: NavAnsatt): Unit = db.transaction {
        repository.navAnsatt.deleteByEntraObjectId(ansatt.entraObjectId)
    }
}
