package no.nav.mulighetsrommet.api.sanity

import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.clients.sanity.SanityResponse
import no.nav.mulighetsrommet.api.veilederflate.Oppskrift
import java.util.UUID

class VeilederflateSanityService(
    private val sanityClient: SanityClient,
) {
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
}
