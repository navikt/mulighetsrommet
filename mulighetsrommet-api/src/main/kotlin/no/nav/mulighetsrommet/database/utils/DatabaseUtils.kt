package no.nav.mulighetsrommet.database.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope

object DatabaseUtils {
    private suspend fun <T> paginateSuspend(pageSize: Int, operation: suspend (Pagination) -> List<T>): Int {
        var page = 1
        var count = 0

        do {
            val items = operation(Pagination.of(page, pageSize))
            page += 1
            count += items.size
        } while (items.isNotEmpty())

        return count
    }

    /**
     * Fan-out utility for å prosessere mange entries i parallell.
     *
     * Et typisk case vil være å paginere over en hel database-tabell ved å implementere en [producer] som henter
     * rader basert på gitt [Pagination] og en [consumer] som prosesserer én enkel rad.
     *
     * Antall konsumenter kan overtyres ved å sette [numConsumers] og [Channel]-kapasiteten til produsenten kan
     * overstyres ved å sette [channelCapacity].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> paginateFanOut(
        producer: (Pagination) -> List<T>,
        numConsumers: Int = 10,
        channelCapacity: Int = 1000,
        consumer: suspend (T) -> Unit,
    ): Int = coroutineScope {
        var totalEntries = 0

        // Produce entries in a separate coroutine
        val events = produce(capacity = channelCapacity) {
            totalEntries = paginateSuspend(channelCapacity) { pagination ->
                val items = producer(pagination)

                items.forEach {
                    send(it)
                }

                items
            }

            close()
        }

        // Create `numConsumers` coroutines to process the entries simultaneously
        (0..numConsumers)
            .map {
                async {
                    events.consumeEach { event ->
                        consumer.invoke(event)
                    }
                }
            }
            .awaitAll()

        totalEntries
    }

    fun String.toFTSPrefixQuery(): String = this
        .trim()
        .split("\\s+".toRegex())
        .mapNotNull { token ->
            val cleaned = token.replace(Regex("""[?`*&|!():'"\\]"""), "")
            if (cleaned.isNotBlank()) "$cleaned:*" else null
        }
        .joinToString(" & ")
}
