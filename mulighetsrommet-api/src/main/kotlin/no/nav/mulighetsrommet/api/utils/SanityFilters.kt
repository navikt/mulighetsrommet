package no.nav.mulighetsrommet.api.utils

import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe

fun utledInnsatsgrupper(innsatsgruppe: Innsatsgruppe): List<Innsatsgruppe> {
    return when (innsatsgruppe) {
        Innsatsgruppe.STANDARD_INNSATS -> listOf(
            Innsatsgruppe.STANDARD_INNSATS,
        )

        Innsatsgruppe.SITUASJONSBESTEMT_INNSATS -> listOf(
            Innsatsgruppe.STANDARD_INNSATS,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
        )

        Innsatsgruppe.SPESIELT_TILPASSET_INNSATS -> listOf(
            Innsatsgruppe.STANDARD_INNSATS,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
        )

        Innsatsgruppe.VARIG_TILPASSET_INNSATS -> listOf(
            Innsatsgruppe.STANDARD_INNSATS,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS,
        )

        Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS -> listOf(
            Innsatsgruppe.STANDARD_INNSATS,
            Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
            Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            Innsatsgruppe.VARIG_TILPASSET_INNSATS,
        )
    }
}
