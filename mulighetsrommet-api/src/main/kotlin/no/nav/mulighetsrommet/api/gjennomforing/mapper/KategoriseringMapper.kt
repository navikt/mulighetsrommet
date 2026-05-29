package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringRequest
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringResponse
import no.nav.mulighetsrommet.api.gjennomforing.kafka.GjennomforingRequestPayload
import no.nav.mulighetsrommet.api.janzz.Sertifisering

object KategoriseringMapper {
    fun fromKafkaPayload(kategorisering: GjennomforingRequestPayload.OpprettEnkeltplass.OpplaringKategorisering): OpplaringKategoriseringRequest {
        return OpplaringKategoriseringRequest(
            kurstypeId = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.KURSTYPE_ID]?.firstOrNull(),
            bransjeId = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.BRANSJE_ID]?.firstOrNull(),
            sertifiseringer = kategorisering.sertifiseringer.map { Sertifisering(it.id, it.navn) }.toSet(),
            forerkort = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.FORERKORT],
            innholdElementer = null, // Ikke støttet
            norskprove = null, // Ikke støttet
            utdanningsprogramId = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.UTDANNINGSPROGRAM_ID]?.firstOrNull(),
            larefag = kategorisering.verdier[OpplaringKategoriseringResponse.Representerer.LAREFAG],
        )
    }
}
