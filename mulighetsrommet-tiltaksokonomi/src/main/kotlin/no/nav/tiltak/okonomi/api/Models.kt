package no.nav.tiltak.okonomi.api

import kotlinx.serialization.Serializable
import no.nav.tiltak.okonomi.model.BestillingStatusType
import no.nav.tiltak.okonomi.model.FakturaStatusType

@Serializable
data class BestillingStatus(
    val bestillingsnummer: String,
    val status: BestillingStatusType,
)

@Serializable
data class SetBestillingStatus(
    val status: BestillingStatusType,
)

@Serializable
data class FakturaStatus(
    val fakturanummer: String,
    val status: FakturaStatusType,
)
