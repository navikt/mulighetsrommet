package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dto.Mutation
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import org.slf4j.LoggerFactory
import java.util.*

class SanityAnsattService(
    private val sanityClient: SanityClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun upsertAnsatt(ansatt: NavAnsattDto) {
        when {
            NavAnsattRolle.KONTAKTPERSON in ansatt.roller -> upsertKontaktperson(ansatt)
            NavAnsattRolle.BETABRUKER in ansatt.roller -> upsertRedaktor(ansatt)
        }
    }

    private suspend fun upsertKontaktperson(ansatt: NavAnsattDto) {
        val queryResponse = sanityClient.query(
            """
            *[_type == "navKontaktperson" && lower(epost) == lower(${ansatt.epost})]
            """.trimIndent(),
        )

        val sanityId = when (queryResponse) {
            is SanityResponse.Result -> Json.decodeFromJsonElement<SanityNavKontaktperson>(queryResponse.result)._id
            is SanityResponse.Error -> UUID.randomUUID()
        }

        val sanityPatch = SanityNavKontaktperson(
            _id = sanityId.toString(),
            enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
            telefonnummer = ansatt.mobilnummer,
            epost = ansatt.epost,
            roller = ansatt.roller.map { it.name }.toList(),
        )

        val response = sanityClient.mutate(
            listOf(
                Mutation(createOrReplace = sanityPatch),
            ),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke upserte kontaktperson i sanity: ${response.status}")
        } else {
            log.info("Oppdaterte kontaktperson i Sanity med id: $sanityId")
        }
    }

    private suspend fun upsertRedaktor(ansatt: NavAnsattDto) {
        val queryResponse = sanityClient.query(
            """
            *[_type == "redaktor" && lower(epost) == lower(${ansatt.epost})]
            """.trimIndent(),
        )

        val sanityId = when (queryResponse) {
            is SanityResponse.Result -> Json.decodeFromJsonElement<SanityRedaktor>(queryResponse.result)._id
            is SanityResponse.Error -> UUID.randomUUID()
        }

        val sanityPatch = SanityRedaktor(
            _id = sanityId.toString(),
            enhet = "${ansatt.hovedenhet.enhetsnummer} ${ansatt.hovedenhet.navn}",
            epost = ansatt.epost,
            roller = ansatt.roller.map { it.name }.toList(),
        )

        val response = sanityClient.mutate(
            listOf(
                Mutation(createOrReplace = sanityPatch),
            ),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke upserte redaktør i sanity: ${response.status}")
        } else {
            log.info("Oppdaterte redaktør i Sanity med id: $sanityId")
        }
    }
}

data class SanityNavKontaktperson(
    val _id: String,
    val enhet: String,
    val telefonnummer: String?,
    val epost: String,
    val roller: List<String>,
)

data class SanityRedaktor(
    val _id: String,
    val enhet: String,
    val epost: String,
    val roller: List<String>,
)
