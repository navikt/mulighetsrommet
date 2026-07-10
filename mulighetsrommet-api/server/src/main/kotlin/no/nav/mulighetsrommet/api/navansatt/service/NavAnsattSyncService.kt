package no.nav.mulighetsrommet.api.navansatt.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.sanity.SanityNavKontaktperson
import no.nav.mulighetsrommet.api.sanity.SanityRedaktor
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.Slug
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class NavAnsattSyncService(
    private val db: ApiDatabase,
    private val navAnsattService: NavAnsattService,
    private val sanityService: SanityService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun synchronizeNavAnsatte(today: LocalDate, deletionDate: LocalDate): Unit = db.session {
        val ansatteToUpsert = navAnsattService.getNavAnsatteForAllRoles()

        logger.info("Oppdaterer ${ansatteToUpsert.size} NavAnsatt fra Azure")
        ansatteToUpsert.forEach { ansatt ->
            val currentAnsattDbo = queries.ansatt.getNavAnsattDbo(ansatt.navIdent)
            NavAnsattDbo.fromNavAnsatt(ansatt).takeIf { it != currentAnsattDbo }?.also {
                queries.ansatt.upsert(it)
            }
            queries.ansatt.setRoller(ansatt.navIdent, ansatt.roller)
        }
        upsertSanityAnsatte(ansatteToUpsert)

        val ansatteEntraObjectIds = ansatteToUpsert.map { it.entraObjectId }
        val ansatteToScheduleForDeletion = queries.ansatt.getAll().filter { ansatt ->
            ansatt.entraObjectId !in ansatteEntraObjectIds && ansatt.skalSlettesDato == null
        }
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting oid=${ansatt.entraObjectId} dato=$deletionDate")
            val ansattToDelete = NavAnsattDbo.fromNavAnsatt(ansatt).copy(skalSlettesDato = deletionDate)
            queries.ansatt.upsert(ansattToDelete)
            queries.ansatt.setRoller(ansattToDelete.navIdent, setOf())
        }

        val ansatteToDelete = queries.ansatt.getAll(skalSlettesDatoLte = today)
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting oid=${ansatt.entraObjectId} dato=${ansatt.skalSlettesDato}")
            deleteNavAnsatt(ansatt)
        }
    }

    private suspend fun deleteNavAnsatt(ansatt: NavAnsatt): Unit = db.transaction {
        queries.ansatt.deleteByEntraObjectId(ansatt.entraObjectId)
        sanityService.removeNavIdentFromTiltaksgjennomforinger(ansatt.navIdent)
        sanityService.deleteNavIdent(ansatt.navIdent)
    }

    private suspend fun upsertSanityAnsatte(ansatte: List<NavAnsatt>) {
        val existingNavKontaktpersonIds = sanityService.getNavKontaktpersoner()
            .associate { it.navIdent.current to it._id }
        val existingRedaktorIds = sanityService.getRedaktorer()
            .associate { it.navIdent.current to it._id }

        logger.info("Upserter ${ansatte.size} ansatte til Sanity")
        val navKontaktpersoner = mutableListOf<SanityNavKontaktperson>()
        val redaktorer = mutableListOf<SanityRedaktor>()
        ansatte.forEach { ansatt ->
            if (ansatt.hasGenerellRolle(Rolle.KONTAKTPERSON)) {
                val id = existingNavKontaktpersonIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                navKontaktpersoner.add(
                    SanityNavKontaktperson(
                        _id = id.toString(),
                        _type = "navKontaktperson",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
                        telefonnummer = ansatt.mobilnummer,
                        epost = ansatt.epost,
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                        enhetsnummer = ansatt.hovedenhet.enhetsnummer,
                    ),
                )
            }

            if (ansatt.hasAnyGenerellRolle(Rolle.AVTALER_SKRIV, Rolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
                val id = existingRedaktorIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                redaktorer.add(
                    SanityRedaktor(
                        _id = id.toString(),
                        _type = "redaktor",
                        enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        epost = Slug(current = ansatt.epost),
                        enhetsnummer = ansatt.hovedenhet.enhetsnummer,
                    ),
                )
            }
        }
        sanityService.createNavKontaktpersoner(navKontaktpersoner)
        sanityService.createRedaktorer(redaktorer)
    }
}
