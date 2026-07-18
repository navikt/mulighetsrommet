package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringRequest
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringResponse
import no.nav.mulighetsrommet.api.gjennomforing.kafka.GjennomforingRequest
import no.nav.mulighetsrommet.api.janzz.Sertifisering

object KategoriseringMapper {
    fun fromKafkaPayload(kategorisering: GjennomforingRequest.OpplaringKategorisering): OpplaringKategoriseringRequest {
        return OpplaringKategoriseringRequest(
            kurstypeId = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.KURSTYPE_ID]?.firstOrNull(),
            bransjeId = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.BRANSJE_ID]?.firstOrNull(),
            sertifiseringer = kategorisering.sertifiseringer.map { Sertifisering(it.id, it.navn) }.toSet(),
            forerkort = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.FORERKORT],
            utdanningsprogramId = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.UTDANNINGSPROGRAM_ID]?.firstOrNull(),
            larefag = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.LAREFAG],
            // Ikke kategorisert hos Komet
            innholdElementer = null,
            norskprove = null,
        )
    }
}
