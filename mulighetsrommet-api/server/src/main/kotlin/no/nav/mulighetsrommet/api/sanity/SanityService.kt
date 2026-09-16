package no.nav.mulighetsrommet.api.sanity

import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.clients.sanity.Mutation
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityTiltakstypeFields
import org.slf4j.LoggerFactory
import java.util.UUID

class SanityService(
    private val sanityClient: SanityClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
}
