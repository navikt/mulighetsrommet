package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import org.slf4j.LoggerFactory
import java.util.*

class SanityTiltaksgjennomforingService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val avtaleRepository: AvtaleRepository,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val virksomhetRepository: VirksomhetRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun deleteSanityTiltaksgjennomforing(sanityId: UUID) {
        val response = sanityClient.mutate(
            listOf(
                // Deletes both drafts and published dokuments
                Mutation<Unit>(delete = Delete(id = "drafts.$sanityId")),
                Mutation(delete = Delete(id = "$sanityId")),
            ),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke slette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Slettet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
    }

    suspend fun createOrPatchSanityTiltaksgjennomforing(tiltaksgjennomforing: TiltaksgjennomforingAdminDto) {
        val avtale = tiltaksgjennomforing.avtaleId?.let { avtaleRepository.get(it) }
        val tiltakstype = tiltakstypeRepository.get(tiltaksgjennomforing.tiltakstype.id)
        val enhet = virksomhetRepository.get(tiltaksgjennomforing.arrangor.organisasjonsnummer).getOrNull()
        val lokasjonForVirksomhetFraBrreg = "${enhet?.postnummer ?: ""} ${enhet?.poststed ?: ""}"

        val sanityTiltaksgjennomforingFields = SanityTiltaksgjennomforingFields(
            tiltaksgjennomforingNavn = tiltaksgjennomforing.navn,
            enheter = tiltaksgjennomforing.navEnheter.map {
                EnhetRef(_ref = "enhet.lokal.${it.enhetsnummer}", _key = it.enhetsnummer)
            },
            fylke = avtale?.navRegion?.enhetsnummer?.let {
                FylkeRef(_ref = "enhet.fylke.$it")
            },
            tiltakstype = tiltakstype?.sanityId?.let { TiltakstypeRef(_ref = it.toString()) },
            tiltaksnummer = tiltaksgjennomforing.tiltaksnummer?.let { TiltaksnummerSlug(current = it) },
            lokasjon = tiltaksgjennomforing.lokasjonArrangor ?: lokasjonForVirksomhetFraBrreg,
        )

        if (tiltaksgjennomforing.sanityId != null) {
            patchSanityTiltaksgjennomforing(tiltaksgjennomforing.sanityId!!, sanityTiltaksgjennomforingFields)
        } else {
            val sanityId = UUID.randomUUID()
            createSanityTiltaksgjennomforing(sanityId, sanityTiltaksgjennomforingFields)

            tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
                tiltaksgjennomforing.id,
                sanityId,
            )
        }
    }

    private suspend fun createSanityTiltaksgjennomforing(
        sanityId: UUID,
        sanityTiltaksgjennomforingFields: SanityTiltaksgjennomforingFields,
    ) {
        val sanityTiltaksgjennomforing = sanityTiltaksgjennomforingFields.toSanityTiltaksgjennomforing(
            id = "drafts.$sanityId", // For å ikke autopublisere dokument i Sanity før redaktør manuelt publiserer
        )

        val response = sanityClient.mutate(
            listOf(Mutation(createOrReplace = sanityTiltaksgjennomforing)),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
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
            throw Exception("Fant ikke sanityId i sanity: $sanityId")
        }

        val response = sanityClient.mutate(
            listOf(Mutation(patch = Patch(id = id, set = sanityTiltaksgjennomforingFields))),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke patche tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Patchet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
    }

    private suspend fun isPublished(sanityId: UUID): Boolean {
        val query = """
            *[_type == "tiltaksgjennomforing" && _id == "$sanityId"]{_id}
        """.trimIndent()
        return when (val response = sanityClient.query(query)) {
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
            *[_type == "tiltaksgjennomforing" && _id == "$sanityId"]{_id}
        """.trimIndent()
        return when (val response = sanityClient.query(query, perspective = SanityPerspective.PREVIEW_DRAFTS)) {
            is SanityResponse.Result -> {
                response.decode<List<JsonObject>>().isNotEmpty()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }
}
