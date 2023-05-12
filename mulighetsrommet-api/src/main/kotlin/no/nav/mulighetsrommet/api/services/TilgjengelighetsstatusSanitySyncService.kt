package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.norg2.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo

@Serializable
data class Tiltaksgjennomforing(
    val _id: String,
    val tiltaksnummer: String?,
)

class TilgjengelighetsstatusSanitySyncService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
) {
    suspend fun synchronizeTilgjengelighetsstatus() =
        coroutineScope {
            val channelCapacity = 20

            val tiltak = produceTiltak(channelCapacity)

            (0..channelCapacity / 2)
                .map {
                    async {
                        writeTilgjengelighetsstatus(tiltak)
                    }
                }
                .awaitAll()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.produceTiltak(capacity: Int): ReceiveChannel<Tiltaksgjennomforing> {
        return produce(capacity = capacity) {
            val result = sanityClient.query(
                """
            *[_type == "tiltaksgjennomforing" && !(_id in path('drafts.**'))]{
              _id,
              "tiltaksnummer": tiltaksnummer.current
            }
                """.trimIndent(),
            )

            if (result is SanityResponse.Result) {
                val gjennomforinger = result.decode<List<Tiltaksgjennomforing>>()
                gjennomforinger.forEach {
                    if (it.tiltaksnummer != null) {
                        send(it)
                    }
                }
            }

            close()
        }
    }

    private suspend fun writeTilgjengelighetsstatus(
        channel: ReceiveChannel<Tiltaksgjennomforing>,
    ) {
        channel.consumeEach { tiltak ->
            val tilgjengelighet =
                tiltak.tiltaksnummer?.let { tiltaksgjennomforingRepository.getTilgjengelighetsstatus(it) }

            tilgjengelighet?.let {
                sanityClient.mutate(
                    """
                {
                    "mutations": [
                        {
                            "patch": {
                                "id": "${tiltak._id}",
                                "set": {
                                    "tilgjengelighetsstatus": "$tilgjengelighet"
                                }
                            }
                        }
                    ]
                }
                    """.trimIndent(),
                )
            }
        }
    }
}
