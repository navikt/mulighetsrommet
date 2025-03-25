package no.nav.mulighetsrommet.api.clients.vedtak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.ZonedDateTimeSerializer
import java.time.ZonedDateTime

@Serializable
data class Gjeldende14aVedtakDto(
    val innsatsgruppe: InnsatsgruppeV2,
    val hovedmal: HovedmalMedOkeDeltakelse?,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val fattetDato: ZonedDateTime,
)
