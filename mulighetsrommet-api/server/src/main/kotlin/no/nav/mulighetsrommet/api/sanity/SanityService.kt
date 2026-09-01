package no.nav.mulighetsrommet.api.sanity

import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.veilederflate.models.Oppskrift
import org.slf4j.LoggerFactory
import java.util.UUID

class SanityService(
    private val sanityClient: SanityClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun getOppskrifter(
        tiltakstypeId: UUID,
        perspective: SanityPerspective,
    ): List<Oppskrift> {
        val query = $$"""
              *[_type == "tiltakstype" && defined(oppskrifter) && _id == $id] {
               oppskrifter[] -> {
                  ...,
                  steg[] {
                    ...,
                    innhold[] {
                      ...,
                      _type == "image" => {
                      ...,
                      asset-> // For å hente ut url til bilder
                      }
                    }
                  }
               }
             }.oppskrifter[]
        """.trimIndent()

        val params = listOf(SanityParam.of("id", tiltakstypeId))

        return when (val result = sanityClient.query(query, params, perspective)) {
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun patchSanityTiltakstype(
        sanityId: UUID,
        navn: String,
    ) {
        val data = SanityTiltakstypeFields(
            tiltakstypeNavn = navn,
        )

        val response = sanityClient.mutate(
            listOf(Mutation.patch(id = sanityId.toString(), set = data)),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke patche tiltakstype med id=$sanityId: ${response.status}")
        } else {
            log.info("Patchet tiltakstype med id=$sanityId")
        }
    }

    suspend fun createSanityEnheter(
        sanityEnheter: List<SanityEnhet>,
    ): SanityClient.MutateResponse {
        val mutations = sanityEnheter.map { Mutation.createOrReplace(it) }
        return sanityClient.mutate(mutations)
    }
}
