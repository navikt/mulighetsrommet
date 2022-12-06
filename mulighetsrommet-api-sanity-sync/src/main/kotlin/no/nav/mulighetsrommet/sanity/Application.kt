package no.nav.mulighetsrommet.sanity

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.serialization.Serializable
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseAdapter
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.hoplite.loadConfiguration

@Serializable
data class Tiltak(
    val _id: String,
    val tiltaksnummer: Int,
)

fun main() {
    val config = loadConfiguration<Config>()

    val db = DatabaseAdapter(config.db)

    val sanity = SanityClient(config.sanity)

    runBlocking {
        synchronizeTilgjengelighetsstatus(db, sanity)
    }
}

private suspend fun synchronizeTilgjengelighetsstatus(db: Database, sanity: SanityClient) =
    coroutineScope {
        val channelCapacity = 20

        val tiltak = produceTiltak(channelCapacity, sanity)

        (0..channelCapacity / 2)
            .map {
                async {
                    writeTilgjengelighetsstatus(tiltak, db, sanity)
                }
            }
            .awaitAll()
    }

@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineScope.produceTiltak(capacity: Int, sanity: SanityClient): ReceiveChannel<Tiltak> {
    return produce(capacity = capacity) {
        val tiltak = sanity.getMany<Tiltak>(
            """
            *[_type == "tiltaksgjennomforing" && !(_id in path('drafts.**'))]{
              _id,
              tiltaksnummer
            }
            """.trimIndent()
        )
        tiltak.result.forEach {
            send(it)
        }
        close()
    }
}

private suspend fun writeTilgjengelighetsstatus(
    channel: ReceiveChannel<Tiltak>,
    db: Database,
    sanity: SanityClient
) {
    channel.consumeEach { tiltak ->
        val tilgjengelighet = queryOf(
            """
            select tilgjengelighet
            from tiltaksgjennomforing_valid
            where tiltaksnummer = ?
            """.trimIndent(),
            tiltak.tiltaksnummer
        )
            .map {
                val value = it.string("tilgjengelighet")
                Tiltaksgjennomforing.Tilgjengelighetsstatus.valueOf(value)
            }
            .asSingle
            .let { db.run(it) }

        tilgjengelighet?.let {
            sanity.mutate(
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
                """.trimIndent()
            )
        }
    }
}
