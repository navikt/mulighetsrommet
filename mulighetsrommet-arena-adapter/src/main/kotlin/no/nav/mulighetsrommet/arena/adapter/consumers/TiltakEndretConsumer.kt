package no.nav.mulighetsrommet.arena.adapter.consumers

import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.arena.adapter.Database
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.utils.ProcessingUtils
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.arena.ArenaTiltak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TiltakEndretConsumer(
    db: Database,
    override val topic: String,
    private val client: MulighetsrommetApiClient
) : TopicConsumer<ArenaTiltak>(db) {

    override val logger: Logger = LoggerFactory.getLogger(TiltakEndretConsumer::class.java)

    override fun toDomain(payload: JsonElement): ArenaTiltak {
        return Json.decodeFromJsonElement(payload.jsonObject["after"]!!)
    }

    override fun resolveKey(payload: ArenaTiltak): String {
        return payload.TILTAKSKODE
    }

    override fun handleEvent(payload: ArenaTiltak) {
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
