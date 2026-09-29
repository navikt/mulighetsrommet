package no.nav.mulighetsrommet.api.totrinnskontroll.kafka

import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.apache.kafka.common.serialization.Deserializer

class TotrinnskontrollHendelseDeserializer : Deserializer<TotrinnskontrollHendelse> {
    override fun deserialize(topic: String, data: ByteArray): TotrinnskontrollHendelse {
        return JsonIgnoreUnknownKeys.decodeFromString<TotrinnskontrollHendelse>(data.decodeToString())
    }
}
