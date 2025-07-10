package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode

@Serializable
data class AvtaltSats(
    val periode: Periode,
    val sats: Int,
)
