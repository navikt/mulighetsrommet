package no.nav.mulighetsrommet.api.navansatt.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.sanity.SanityNavKontaktperson
import no.nav.mulighetsrommet.api.sanity.SanityRedaktor
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.Slug
import no.nav.mulighetsrommet.model.NavEnhetNummer
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
            val current = queries.ansatt.get(ansatt.navIdent)
            if (ansatt != current) {
                queries.ansatt.save(ansatt)
            }
        }
        upsertSanityAnsatte(ansatteToUpsert, queries.enhet.getAll().associate { it.enhetsnummer to it.navn })

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

    private suspend fun deleteNavAnsatt(ansatt: NavAnsatt): Unit = db.transaction {
        queries.ansatt.deleteByEntraObjectId(ansatt.entraObjectId)
        sanityService.removeNavIdentFromTiltaksgjennomforinger(ansatt.navIdent)
        sanityService.deleteNavIdent(ansatt.navIdent)
    }

    private suspend fun upsertSanityAnsatte(ansatte: List<NavAnsatt>, enhetNavnByNummer: Map<NavEnhetNummer, String>) {
        val existingNavKontaktpersonIds = sanityService.getNavKontaktpersoner()
            .associate { it.navIdent.current to it._id }
        val existingRedaktorIds = sanityService.getRedaktorer()
            .associate { it.navIdent.current to it._id }

        logger.info("Upserter ${ansatte.size} ansatte til Sanity")
        val navKontaktpersoner = mutableListOf<SanityNavKontaktperson>()
        val redaktorer = mutableListOf<SanityRedaktor>()
        ansatte.forEach { ansatt ->
            val hovedenhetNavn = enhetNavnByNummer[ansatt.hovedenhet]

            if (ansatt.hasGenerellRolle(Rolle.KONTAKTPERSON)) {
                val id = existingNavKontaktpersonIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                navKontaktpersoner.add(
                    SanityNavKontaktperson(
                        _id = id.toString(),
                        _type = "navKontaktperson",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        enhet = "${ansatt.hovedenhet} $hovedenhetNavn",
                        telefonnummer = ansatt.mobilnummer,
                        epost = ansatt.epost,
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                        enhetsnummer = ansatt.hovedenhet,
                    ),
                )
            }

            if (ansatt.hasAnyGenerellRolle(Rolle.AVTALER_SKRIV, Rolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
                val id = existingRedaktorIds[ansatt.navIdent.value] ?: UUID.randomUUID()
                redaktorer.add(
                    SanityRedaktor(
                        _id = id.toString(),
                        _type = "redaktor",
                        enhet = "${ansatt.hovedenhet} $hovedenhetNavn",
                        navn = "${ansatt.fornavn} ${ansatt.etternavn}",
                        navIdent = Slug(current = ansatt.navIdent.value),
                        epost = Slug(current = ansatt.epost),
                        enhetsnummer = ansatt.hovedenhet,
                    ),
                )
            }
        }
        sanityService.createNavKontaktpersoner(navKontaktpersoner)
        sanityService.createRedaktorer(redaktorer)
    }
}
