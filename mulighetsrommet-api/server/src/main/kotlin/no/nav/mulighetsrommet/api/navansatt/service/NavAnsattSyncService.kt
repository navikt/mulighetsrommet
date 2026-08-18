package no.nav.mulighetsrommet.api.navansatt.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import org.slf4j.LoggerFactory
import java.time.LocalDate

class NavAnsattSyncService(
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun synchronizeNavAnsatte(today: LocalDate, deletionDate: LocalDate): Unit = db.session {
        val ansatteToUpsert = navAnsattService.getNavAnsatteForAllRoles()

        logger.info("Oppdaterer ${ansatteToUpsert.size} NavAnsatt fra Azure")
        ansatteToUpsert.forEach { ansatt ->
            val current = queries.ansatt.get(ansatt.navIdent)
            if (ansatt != current) {
                queries.ansatt.save(ansatt)
            }
        }

        val ansatteEntraObjectIds = ansatteToUpsert.map { it.entraObjectId }
        val ansatteToScheduleForDeletion = queries.ansatt.getAll().filter { ansatt ->
            ansatt.skalSlettesDato == null && ansatt.entraObjectId !in ansatteEntraObjectIds
        }
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting oid=${ansatt.entraObjectId} dato=$deletionDate")
            queries.ansatt.save(ansatt.skalSlettes(deletionDate))
        }

        val ansatteToDelete = queries.ansatt.getAll().filter { ansatt ->
            val skalSlettesDato = ansatt.skalSlettesDato
            skalSlettesDato != null && skalSlettesDato <= today
        }
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting oid=${ansatt.entraObjectId} dato=${ansatt.skalSlettesDato}")
            deleteNavAnsatt(ansatt)
        }
    }

    private fun deleteNavAnsatt(ansatt: NavAnsatt): Unit = db.transaction {
        queries.ansatt.deleteByEntraObjectId(ansatt.entraObjectId)
    }
}
