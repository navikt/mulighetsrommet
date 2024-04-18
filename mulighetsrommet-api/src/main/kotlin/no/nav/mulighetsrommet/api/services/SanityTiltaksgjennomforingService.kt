package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotliquery.Session
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import org.slf4j.LoggerFactory
import java.util.*

class SanityTiltaksgjennomforingService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltakstypeRepository: TiltakstypeRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun deleteSanityTiltaksgjennomforing(sanityId: UUID) {
        val response = sanityClient.mutate(
            listOf(
                // Deletes both drafts and published dokuments
                Mutation.delete(id = "drafts.$sanityId"),
                Mutation.delete(id = "$sanityId"),
            ),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke slette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Slettet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
    }

    suspend fun createOrPatchSanityTiltaksgjennomforing(
        tiltaksgjennomforing: TiltaksgjennomforingAdminDto,
        tx: Session,
    ) {
        val tiltakstype = tiltakstypeRepository.get(tiltaksgjennomforing.tiltakstype.id)

        val sanityTiltaksgjennomforingFields = SanityTiltaksgjennomforingFields(
            tiltaksgjennomforingNavn = tiltaksgjennomforing.navn,
            tiltakstype = tiltakstype?.sanityId?.let { TiltakstypeRef(_ref = it.toString()) },
            tiltaksnummer = tiltaksgjennomforing.tiltaksnummer?.let { TiltaksnummerSlug(current = it) },
            stedForGjennomforing = tiltaksgjennomforing.stedForGjennomforing,
        )

        if (tiltaksgjennomforing.sanityId != null) {
            patchSanityTiltaksgjennomforing(tiltaksgjennomforing.sanityId, sanityTiltaksgjennomforingFields)
        } else {
            val sanityId = UUID.randomUUID()
            createSanityTiltaksgjennomforing(sanityId, sanityTiltaksgjennomforingFields)

            tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
                tiltaksgjennomforing.id,
                sanityId,
                tx,
            )
        }
    }

    private suspend fun createSanityTiltaksgjennomforing(
        sanityId: UUID,
        sanityTiltaksgjennomforingFields: SanityTiltaksgjennomforingFields,
    ) {
        val sanityTiltaksgjennomforing = sanityTiltaksgjennomforingFields.toSanityTiltaksgjennomforing(
            // For å ikke autopublisere dokument i Sanity før redaktør manuelt publiserer
            id = "drafts.$sanityId",
        )

        val response = sanityClient.mutate(
            listOf(Mutation.createOrReplace(sanityTiltaksgjennomforing)),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke opprette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Opprettet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
    }

    private suspend fun patchSanityTiltaksgjennomforing(
        sanityId: UUID,
        sanityTiltaksgjennomforingFields: SanityTiltaksgjennomforingFields,
    ) {
        val id = if (isPublished(sanityId)) {
            "$sanityId"
        } else if (isDraft(sanityId)) {
            "drafts.$sanityId"
        } else {
            // Dette betyr at gjennomføringen er slettet i Sanity. Så vi prøver ikke patche
            return
        }

        val response = sanityClient.mutate(
            listOf(Mutation.patch(id = id, set = sanityTiltaksgjennomforingFields)),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke patche tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Patchet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
    }

    private suspend fun isPublished(sanityId: UUID): Boolean {
        val query = """
            *[_type == "tiltaksgjennomforing" && _id == ${'$'}id]{_id}
        """.trimIndent()

        val params = listOf(SanityParam.of("id", sanityId))

        return when (val response = sanityClient.query(query, params)) {
            is SanityResponse.Result -> {
                response.decode<List<JsonObject>>().isNotEmpty()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }

    private suspend fun isDraft(sanityId: UUID): Boolean {
        val query = """
            *[_type == "tiltaksgjennomforing" && _id == ${'$'}id]{_id}
        """.trimIndent()

        val params = listOf(SanityParam.of("id", sanityId))

        return when (val response = sanityClient.query(query, params, SanityPerspective.PREVIEW_DRAFTS)) {
            is SanityResponse.Result -> {
                response.decode<List<JsonObject>>().isNotEmpty()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }
}
