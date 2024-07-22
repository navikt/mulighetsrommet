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
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import org.slf4j.LoggerFactory
import java.util.*

class SanityTiltakService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tiltakstypeRepository: TiltakstypeRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun patchSanityTiltakstype(tiltakstype: TiltakstypeAdminDto) {
        val sanityId = requireNotNull(tiltakstype.sanityId) {
            "Kan ikke patche tiltakstype med id=${tiltakstype.id} fordi den mangler en referanse til dokument i Sanity"
        }

        val data = SanityTiltakstypeFields(
            tiltakstypeNavn = tiltakstype.navn,
            innsatsgrupper = tiltakstype.innsatsgrupper,
        )

        patchSanityTiltakstype(sanityId, data)
    }

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
        tiltaksgjennomforing: ArenaTiltaksgjennomforingDbo,
        tx: Session,
    ): UUID? {
        val tiltakstype = tiltakstypeRepository.get(tiltaksgjennomforing.tiltakstypeId)

        val sanityTiltaksgjennomforingFields = SanityTiltaksgjennomforingFields(
            tiltaksgjennomforingNavn = tiltaksgjennomforing.navn,
            tiltakstype = tiltakstype?.sanityId?.let { TiltakstypeRef(_ref = it.toString()) },
            tiltaksnummer = TiltaksnummerSlug(current = tiltaksgjennomforing.tiltaksnummer),
        )

        val sanityId = tiltaksgjennomforing.sanityId
            // Fallback mens egen regi tiltak fortsatt er i api, og sanity_id ikke har blitt flyttet
            // over til arena-adapter (som gjøres via denne metoden)
            ?: tiltaksgjennomforingRepository.getSanityTiltaksgjennomforingId(tiltaksgjennomforing.id, tx)

        return if (sanityId != null) {
            patchSanityTiltaksgjennomforing(sanityId, sanityTiltaksgjennomforingFields)
            sanityId
        } else {
            val newSanityId = UUID.randomUUID()

            createSanityTiltaksgjennomforing(newSanityId, sanityTiltaksgjennomforingFields)

            newSanityId
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

    private suspend fun patchSanityTiltakstype(
        sanityId: UUID,
        data: SanityTiltakstypeFields,
    ) {
        val id = requireNotNull(getSanityIdStatus("tiltakstype", sanityId)) {
            "Fant ikke tiltakstype i Sanity med id=$sanityId"
        }

        val response = sanityClient.mutate(
            listOf(Mutation.patch(id = id, set = data)),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke patche tiltakstype med id=$sanityId: ${response.status}")
        } else {
            log.info("Patchet tiltakstype med id=$sanityId")
        }
    }

    private suspend fun patchSanityTiltaksgjennomforing(
        sanityId: UUID,
        sanityTiltaksgjennomforingFields: SanityTiltaksgjennomforingFields,
    ) {
        val id = getSanityIdStatus("tiltaksgjennomforing", sanityId) ?: run {
            log.info("Lar være å patche gjennomføring med sanityId=$sanityId fordi den har blitt slettet manuelt")
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

    private suspend fun getSanityIdStatus(type: String, sanityId: UUID) =
        if (isPublished(type, sanityId)) {
            "$sanityId"
        } else if (isDraft(type, sanityId)) {
            "drafts.$sanityId"
        } else {
            null
        }

    private suspend fun isPublished(type: String, sanityId: UUID): Boolean {
        val query = """
            *[_type == ${'$'}type && _id == ${'$'}id]{_id}
        """.trimIndent()

        val params = listOf(SanityParam.of("type", type), SanityParam.of("id", sanityId))

        return when (val response = sanityClient.query(query, params)) {
            is SanityResponse.Result -> {
                response.decode<List<JsonObject>>().isNotEmpty()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved oppslag på '$type' med id '$sanityId': ${response.error}")
            }
        }
    }

    private suspend fun isDraft(type: String, sanityId: UUID): Boolean {
        val query = """
            *[_type == ${'$'}type && _id == ${'$'}id]{_id}
        """.trimIndent()

        val params = listOf(SanityParam.of("type", type), SanityParam.of("id", sanityId))

        return when (val response = sanityClient.query(query, params, SanityPerspective.PREVIEW_DRAFTS)) {
            is SanityResponse.Result -> {
                response.decode<List<JsonObject>>().isNotEmpty()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved oppslag på '$type' med id '$sanityId': ${response.error}")
            }
        }
    }
}
