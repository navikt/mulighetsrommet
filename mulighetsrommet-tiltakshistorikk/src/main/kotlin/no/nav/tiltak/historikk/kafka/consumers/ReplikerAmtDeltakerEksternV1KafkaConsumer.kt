package no.nav.tiltak.historikk.kafka.consumers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.tiltak.historikk.model.KometDeltaker
import no.nav.tiltak.historikk.replikering.KometDeltakerReplikering
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class ReplikerAmtDeltakerEksternV1KafkaConsumer(
    private val replikering: KometDeltakerReplikering,
    private val clock: Clock = Clock.system(ZoneId.of("Europe/Oslo")),
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    override suspend fun consume(key: UUID, message: JsonElement) {
        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerEksternV1Dto?>(message)
            ?: return replikering.delete(key)
        replikering.upsert(amtDeltaker.toKometDeltaker(LocalDate.now(clock)))
    }
}

fun AmtDeltakerEksternV1Dto.toKometDeltaker(dato: LocalDate): KometDeltaker {
    val mengder = deltakelsesmengder.sortedWith(
        compareBy<AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto> { it.gyldigFraDato }
            .thenBy { it.opprettetTidspunkt },
    )
    val gjeldendeDato = minOf(dato, sluttDato ?: dato)
    val mengde = mengder.lastOrNull { it.gyldigFraDato <= gjeldendeDato } ?: mengder.firstOrNull()
    return KometDeltaker(
        id = id,
        gjennomforingId = gjennomforingId,
        personIdent = personIdent,
        startDato = startDato,
        sluttDato = sluttDato,
        statusType = status.type,
        statusOpprettetTidspunkt = status.opprettetTidspunkt,
        statusAarsak = status.aarsak.type,
        opprettetTidspunkt = registrertTidspunkt,
        oppdatertTidspunkt = endretTidspunkt,
        dagerPerUke = mengde?.dagerPerUke,
        prosentStilling = mengde?.deltakelsesprosent,
    )
}
