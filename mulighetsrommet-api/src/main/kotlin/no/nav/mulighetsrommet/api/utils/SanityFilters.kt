package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.api.clients.vedtak.Innsatsgruppe

fun utledInnsatsgrupper(innsatsgruppe: String?): List<String> {
    return when (innsatsgruppe) {
        Innsatsgruppe.STANDARD_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
        )

        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
        )

        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
        )

        Innsatsgruppe.VARIG_TILPASSET_INNSATS.name -> listOf(
            Innsatsgruppe.STANDARD_INNSATS.name,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS.name,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS.name,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS.name,
        )

        else -> emptyList()
    }
}
