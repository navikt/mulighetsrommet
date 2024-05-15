package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.ssb.SsbNusClient
import no.nav.mulighetsrommet.api.repositories.NusElement
import no.nav.mulighetsrommet.api.repositories.SsbNusRepository
import no.nav.mulighetsrommet.domain.Tiltakskode

class SsbNusService(val client: SsbNusClient, private val ssbNusRepository: SsbNusRepository) {
    suspend fun syncData(version: String) {
        val data = client.fetchNusData(version)
        ssbNusRepository.upsert(data, version)
    }

    fun getNusData(tiltakskode: Tiltakskode, version: String): Map<String, List<NusElement>> {
        val data = ssbNusRepository.getNusData(tiltakskode, version)

        return data.groupBy {
            val parent = data.find { el -> el.code == it.parent }
            parent?.name ?: it.name
        }
    }
}
