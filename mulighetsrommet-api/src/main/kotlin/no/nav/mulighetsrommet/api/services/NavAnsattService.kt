package no.nav.mulighetsrommet.api.services

import arrow.core.toNonEmptyListOrNull
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.Mutation
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.utils.EnhetFilter
import no.nav.mulighetsrommet.api.utils.NavAnsattFilter
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.notifications.NotificationMetadata
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationType
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*

class NavAnsattService(
    private val roles: List<AdGruppeNavAnsattRolleMapping>,
    private val db: Database,
    private val microsoftGraphService: MicrosoftGraphService,
    private val navAnsattRepository: NavAnsattRepository,
    private val sanityClient: SanityClient,
    private val avtaleRepository: AvtaleRepository,
    private val navEnhetService: NavEnhetService,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSynchronizeNavAnsatt(azureId: UUID): NavAnsattDto {
        return navAnsattRepository.getByAzureId(azureId) ?: run {
            logger.info("Fant ikke NavAnsatt for azureId=$azureId i databasen, forsøker Azure AD i stedet")
            val ansatt = getNavAnsattFromAzure(azureId)
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansatt))
            ansatt
        }
    }

    fun getNavAnsatte(filter: NavAnsattFilter): List<NavAnsattDto> {
        return navAnsattRepository.getAll(roller = filter.roller)
    }

    suspend fun getNavAnsattFromAzure(azureId: UUID): NavAnsattDto {
        val rolesDirectory = roles.associateBy { it.adGruppeId }

        val roller = microsoftGraphService.getNavAnsattAdGrupper(azureId, AccessType.M2M)
            .filter { rolesDirectory.containsKey(it.id) }
            .map { rolesDirectory.getValue(it.id).rolle }
            .toSet()

        if (roller.isEmpty()) {
            logger.info("Ansatt med azureId=$azureId har ingen av rollene $roles")
            throw IllegalStateException("Ansatt med azureId=$azureId har ingen av de påkrevde rollene")
        }

        val ansatt = microsoftGraphService.getNavAnsatt(azureId, AccessType.M2M)
        return NavAnsattDto.fromAzureAdNavAnsatt(ansatt, roller)
    }

    suspend fun getNavAnsatteFromAzure(): List<NavAnsattDto> {
        return roles
            .flatMap {
                val members = microsoftGraphService.getNavAnsatteInGroup(it.adGruppeId)
                logger.info("Fant ${members.size} i AD gruppe id=${it.adGruppeId}")
                members.map { ansatt ->
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt, setOf(it.rolle))
                }
            }
            .groupBy { it.navIdent }
            .map { (_, value) ->
                value.reduce { a1, a2 ->
                    a1.copy(roller = a1.roller + a2.roller)
                }
            }
    }

    suspend fun synchronizeNavAnsatte(today: LocalDate, deletionDate: LocalDate) {
        val ansatteToUpsert = getNavAnsatteFromAzure()

        logger.info("Oppdaterer ${ansatteToUpsert.size} NavAnsatt fra Azure")
        ansatteToUpsert.forEach { ansatt ->
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansatt))
        }
        upsertSanityAnsatte(ansatteToUpsert)

        val ansatteAzureIds = ansatteToUpsert.map { it.azureId }
        val ansatteToScheduleForDeletion = navAnsattRepository.getAll().filter { ansatt ->
            ansatt.azureId !in ansatteAzureIds && ansatt.skalSlettesDato == null
        }
        ansatteToScheduleForDeletion.forEach { ansatt ->
            logger.info("Oppdaterer NavAnsatt med dato for sletting azureId=${ansatt.azureId} dato=$deletionDate")
            val ansattToDelete = ansatt.copy(roller = emptySet(), skalSlettesDato = deletionDate)
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansattToDelete))
        }

        val ansatteToDelete = navAnsattRepository.getAll(skalSlettesDatoLte = today)
        ansatteToDelete.forEach { ansatt ->
            logger.info("Sletter NavAnsatt fordi vi har passert dato for sletting azureId=${ansatt.azureId} dato=${ansatt.skalSlettesDato}")
            deleteNavAnsatt(ansatt)
        }
    }

    private suspend fun deleteNavAnsatt(ansatt: NavAnsattDto) {
        val avtaleIds = avtaleRepository.getAvtaleIdsByAdministrator(ansatt.navIdent)

        db.transactionSuspend { tx ->
            navAnsattRepository.deleteByAzureId(ansatt.azureId, tx)
            deleteSanityAnsatt(ansatt)
        }

        avtaleIds
            .forEach {
                val avtale = requireNotNull(avtaleRepository.get(it))
                if (avtale.administratorer.isEmpty()) {
                    notifyRelevantAdministrators(avtale, ansatt.hovedenhet)
                }
            }
    }

    private fun notifyRelevantAdministrators(
        avtale: AvtaleAdminDto,
        hovedenhet: NavAnsattDto.Hovedenhet,
    ) {
        val region = navEnhetService.hentOverordnetFylkesenhet(hovedenhet.enhetsnummer)
            ?: return

        val potentialAdministratorHovedenheter = navEnhetService.hentAlleEnheter(
            EnhetFilter(
                statuser = listOf(NavEnhetStatus.AKTIV),
                overordnetEnhet = region.enhetsnummer,
            ),
        )
            .map { it.enhetsnummer }
            .plus(region.enhetsnummer)

        val administrators = navAnsattRepository
            .getAll(
                roller = listOf(NavAnsattRolle.AVTALER_SKRIV),
                hovedenhetIn = potentialAdministratorHovedenheter,
            )
            .map { it.navIdent }
            .toNonEmptyListOrNull() ?: return

        val notification = ScheduledNotification(
            type = NotificationType.TASK,
            title = """Avtalen "${avtale.navn}" mangler administrator.""",
            description = "Du har blitt varslet fordi din NAV-hovedenhet er i samme fylke som forrige administrators NAV-hovedenhet. Gå til avtalen og sett deg som administrator hvis du eier avtalen.",
            metadata = NotificationMetadata(
                linkText = "Gå til avtalen",
                link = "/avtaler/${avtale.id}",
            ),
            targets = administrators,
            createdAt = Instant.now(),
        )
        notificationService.scheduleNotification(notification)
    }

    private suspend fun deleteSanityAnsatt(ansatt: NavAnsattDto) {
        val queryResponse = sanityClient.query(
            """
            *[_type == "navKontaktperson" && navIdent.current == ${'$'}navIdent || _type == "redaktor" && navIdent.current == ${'$'}navIdent]._id
            """.trimIndent(),
            params = listOf(SanityParam.of("navIdent", ansatt.navIdent.value)),
        )

        val ider = when (queryResponse) {
            is SanityResponse.Result -> queryResponse.decode<List<String>>()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut id'er til sletting fra Sanity: ${queryResponse.error}")
        }

        if (ider.isEmpty()) {
            return
        }

        val result = sanityClient.mutate(mutations = ider.map { Mutation.delete(it) })
        if (result.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke slette Sanity-dokument: ${result.bodyAsText()}")
        }
    }

    private suspend fun upsertSanityAnsatte(ansatte: List<NavAnsattDto>) {
        val navKontaktpersonResponse = sanityClient.query(
            """
            *[_type == "navKontaktperson"]
            """.trimIndent(),
        )

        val redaktorResponse = sanityClient.query(
            """
            *[_type == "redaktor"]
            """.trimIndent(),
        )

        val kontaktpersoner = when (navKontaktpersonResponse) {
            is SanityResponse.Result -> navKontaktpersonResponse.decode<List<SanityNavKontaktperson>?>()
                ?.associate { it.navIdent.current to it._id } ?: emptyMap()

            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut kontaktpersoner fra Sanity: ${navKontaktpersonResponse.error}")
        }

        val redaktorer = when (redaktorResponse) {
            is SanityResponse.Result -> redaktorResponse.decode<List<SanityRedaktor>?>()
                ?.associate { it.navIdent.current to it._id } ?: emptyMap()

            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut redaktører fra Sanity: ${redaktorResponse.error}")
        }

        logger.info("Upserter ${ansatte.size} ansatte til Sanity")
        val kontaktpersonMutations = mutableListOf<Mutation<SanityNavKontaktperson>>()
        val redaktorMutations = mutableListOf<Mutation<SanityRedaktor>>()
        ansatte.forEach { ansatt ->
            if (ansatt.roller.contains(NavAnsattRolle.KONTAKTPERSON)) {
                val id = kontaktpersoner[ansatt.navIdent.value] ?: UUID.randomUUID()
                val mutation = createSanityNavKontaktpersonMutation(ansatt, id.toString())
                kontaktpersonMutations.add(mutation)
            }

            if (ansatt.roller.contains(NavAnsattRolle.AVTALER_SKRIV) || ansatt.roller.contains(NavAnsattRolle.TILTAKSGJENNOMFORINGER_SKRIV)) {
                val id = redaktorer[ansatt.navIdent.value] ?: UUID.randomUUID()
                redaktorMutations.add(upsertRedaktor(ansatt, id.toString()))
            }
        }
        upsertMutations(kontaktpersonMutations, redaktorMutations)
    }

    private fun createSanityNavKontaktpersonMutation(
        ansatt: NavAnsattDto,
        id: String,
    ): Mutation<SanityNavKontaktperson> {
        val sanityPatch = SanityNavKontaktperson(
            _id = id,
            _type = "navKontaktperson",
            navIdent = Slug(current = ansatt.navIdent.value),
            enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
            telefonnummer = ansatt.mobilnummer,
            epost = ansatt.epost,
            navn = "${ansatt.fornavn} ${ansatt.etternavn}",
        )

        return Mutation.createOrReplace(sanityPatch)
    }

    private fun upsertRedaktor(ansatt: NavAnsattDto, id: String): Mutation<SanityRedaktor> {
        val sanityPatch = SanityRedaktor(
            _id = id,
            _type = "redaktor",
            enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
            navn = "${ansatt.fornavn} ${ansatt.etternavn}",
            navIdent = Slug(current = ansatt.navIdent.value),
            epost = Slug(current = ansatt.epost),
        )
        return Mutation.createOrReplace(sanityPatch)
    }

    private suspend fun upsertMutations(
        kontaktpersoner: List<Mutation<SanityNavKontaktperson>>,
        redaktorer: List<Mutation<SanityRedaktor>>,
    ) {
        val kontaktpersonMutationResponse = sanityClient.mutate(kontaktpersoner)
        val redaktorMutationResponse = sanityClient.mutate(redaktorer)
        checkResponse(kontaktpersonMutationResponse)
        checkResponse(redaktorMutationResponse)
    }

    private suspend fun checkResponse(response: HttpResponse) {
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke upserte mutations: Error: ${response.bodyAsText()} - Status: ${response.status}")
        }
        logger.info("Upsert mutations til Sanity ${response.status}")
    }
}

@Serializable
data class SanityNavKontaktperson(
    val _id: String,
    val _type: String,
    val navIdent: Slug,
    val enhet: String,
    val telefonnummer: String? = null,
    val epost: String,
    val navn: String,
)

@Serializable
data class SanityRedaktor(
    val _id: String,
    val _type: String,
    val navIdent: Slug,
    val enhet: String,
    val epost: Slug,
    val navn: String,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Slug(
    @EncodeDefault
    val _type: String = "slug",
    val current: String,
)
