package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AvtaltSats(
    @Serializable(with = LocalDateSerializer::class)
    val gjelderFra: LocalDate,
    val sats: Int,
)
