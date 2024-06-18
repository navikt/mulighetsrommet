package no.nav.mulighetsrommet.api.services.utdanning

import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient

class UtdanningService(private val utdanningClient: UtdanningClient) {
    suspend fun syncUtdanning() {
        val utdanninger = utdanningClient.getUtdanninger()
        println(utdanninger) // TODO Lagre utdanninger i databasen
    }
}
