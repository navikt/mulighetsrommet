package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.arena.ArenaTiltak
import org.slf4j.LoggerFactory

class TiltakEndretConsumer(
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaTiltak, ArenaTiltak>() {

    private val logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)

    override fun toDomain(payload: String): ArenaTiltak {
        return Json.decodeFromJsonElement(Json.parseToJsonElement(payload).jsonObject["after"]!!)
    }

    override fun resolveKey(payload: ArenaTiltak): String {
        return payload.TILTAKSKODE
    }

    override fun processEvent(payload: ArenaTiltak) {
        client.sendRequest(HttpMethod.Put, "/api/v1/arena/tiltakstyper", payload.toAdapterTiltak())
        logger.debug("processed tiltak endret event")
    }

    private fun ArenaTiltak.toAdapterTiltak() = AdapterTiltak(
        navn = this.TILTAKSNAVN,
        innsatsgruppe = ProcessingUtils.toInnsatsgruppe(this.TILTAKSKODE),
        tiltakskode = this.TILTAKSKODE,
        fraDato = ProcessingUtils.getArenaDateFromTo(this.DATO_FRA),
        tilDato = ProcessingUtils.getArenaDateFromTo(this.DATO_TIL)
    )

}
