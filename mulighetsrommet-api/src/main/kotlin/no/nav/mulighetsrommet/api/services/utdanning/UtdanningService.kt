package no.nav.mulighetsrommet.api.services.utdanning

import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient

class UtdanningService(private val utdanningClient: UtdanningClient) {
    suspend fun syncUtdanning() {
        utdanningClient.getUtdanninger()
    }
}
