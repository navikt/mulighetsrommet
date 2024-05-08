package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.ssb.SsbNusClient
import no.nav.mulighetsrommet.api.repositories.SsbNusRepository

class SsbNusService(val client: SsbNusClient, private val ssbNusRepository: SsbNusRepository) {
    suspend fun syncData(version: String) {
        val data = client.fetchNusData(version)
        ssbNusRepository.upsert(data, version)
    }
}
