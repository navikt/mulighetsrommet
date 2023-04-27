package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.Serializable
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.NavEnhetUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

@Serializable
data class Tiltak(
    val _id: String,
    val tiltaksnummer: String?,
)

class TilgjengelighetsstatusSanitySyncService(
    private val sanityClient: SanityClient,
    private val slackNotifier: SlackNotifier,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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
    private fun CoroutineScope.produceTiltak(capacity: Int): ReceiveChannel<Tiltak> {
        return produce(capacity = capacity) {
            val tiltak = sanityClient.getMany<Tiltak>(
                """
            *[_type == "tiltaksgjennomforing" && !(_id in path('drafts.**'))]{
              _id,
              "tiltaksnummer": tiltaksnummer.current
            }
            """.trimIndent(),
            )
            tiltak.result.forEach {
                if (it.tiltaksnummer != null) {
                    send(it)
                }
            }
            close()
        }
    }

    private suspend fun writeTilgjengelighetsstatus(
        channel: ReceiveChannel<Tiltak>,
    ) {
        channel.consumeEach { tiltak ->
            val tilgjengelighet = tiltaksgjennomforingRepository.getTilgjengelighetsstatus(tiltak.tiltaksnummer)

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
