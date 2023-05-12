package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class SanityTiltaksgjennomforingService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun syncTiltaksgjennomforingerTilSanity(dryRun: Boolean) {
        val gjennomforingerSomMangler = tiltaksgjennomforingRepository.getTiltaksgjennomforingerUtenSanityId()
            .getOrThrow()
        val opprettet = gjennomforingerSomMangler
            .sumOf { opprettSanityTiltaksgjennomforing(it, dryRun) }
        log.info(
            "Lagde $opprettet Sanity dokumenter for ${gjennomforingerSomMangler.size}" +
            " tiltaksgjennomforinger som manglet sanity_id",
        )
    }

    private suspend fun oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing: TiltaksgjennomforingAdminDto): Boolean {
        val tiltaksnummer = tiltaksgjennomforing.tiltaksnummer ?: return false

        val sanityTiltaksgjennomforinger = hentTiltaksgjennomforinger(tiltaksnummer)
        if (sanityTiltaksgjennomforinger.size > 1) {
            throw Exception("Fant ${sanityTiltaksgjennomforinger.size} sanity dokumenter med tiltaksnummer: $tiltaksnummer")
        }
        if (sanityTiltaksgjennomforinger.isEmpty()) {
            return false
        }
        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
            tiltaksgjennomforing.id,
            sanityTiltaksgjennomforinger[0]._id,
        ).getOrThrow()
        return true
    }

    suspend fun opprettSanityTiltaksgjennomforing(
        tiltaksgjennomforing: TiltaksgjennomforingAdminDto,
        dryRun: Boolean = false,
    ): Int {
        if (oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing)) {
            return 0
        }

        val sanityTiltaksgjennomforingId = UUID.randomUUID().toString()
        val sanityTiltaksgjennomforing = SanityTiltaksgjennomforing(
            _id = sanityTiltaksgjennomforingId,
            tiltaksgjennomforingNavn = tiltaksgjennomforing.navn,
        )
        val response = sanityClient.mutate(
            Mutations(mutations = listOf(Mutation(createIfNotExists = sanityTiltaksgjennomforing))),
            dryRun = dryRun
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke opprette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Opprettet tiltaksgjennomforing i Sanity med id: $sanityTiltaksgjennomforingId")
        }

        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(tiltaksgjennomforing.id, sanityTiltaksgjennomforingId)
            .getOrThrow()

        return 1
    }

    private suspend fun hentTiltaksgjennomforinger(tiltaksnummer: String): List<SanityTiltaksgjennomforingResponse> {
        val query = """
            *[_type == "tiltaksgjennomforing" &&
            !(_id in path('drafts.**')) &&
            (tiltaksnummer.current == "$tiltaksnummer" || tiltaksnummer.current == "${tiltaksnummer.split("#").getOrNull(1)}")]
        """.trimIndent()
        return when (val response = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                JsonIgnoreUnknownKeys.decodeFromJsonElement(response.result)
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }
}
