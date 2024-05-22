package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusClient
import no.nav.mulighetsrommet.api.repositories.NusElement
import no.nav.mulighetsrommet.api.repositories.SsbNusRepository
import no.nav.mulighetsrommet.domain.Tiltakskode

class SsbNusService(val client: SsbNusClient, private val ssbNusRepository: SsbNusRepository) {
    suspend fun syncData(version: String) {
        val data = client.fetchNusData(version)
        ssbNusRepository.upsert(data, version)
    }

    fun getNusData(tiltakskode: Tiltakskode, version: String): NusDataResponse {
        val (overordneteKategorier, underkategorier) = ssbNusRepository
            .getNusData(tiltakskode, version)
            .partition { it.parent == null }

        return NusDataResponse(
            data = overordneteKategorier.map { overordnet ->
                NusData(
                    nivaa = overordnet.name,
                    kategorier = underkategorier.filter { it.code.startsWith(overordnet.code) },
                )
            },
        )
    }
}

@Serializable
data class NusDataResponse(
    val data: List<NusData>,
)

@Serializable
data class NusData(
    val nivaa: String,
    val kategorier: List<NusElement>,
)
