package no.nav.mulighetsrommet.api.clients.vedtak

import kotlinx.serialization.Serializable

@Serializable
data class VedtakDto(
    val innsatsgruppe: Innsatsgruppe,
) {
    enum class Innsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        SPESIELT_TILPASSET_INNSATS,
        VARIG_TILPASSET_INNSATS,

        /**
         * Denne innsatsgruppen er definert i veilarbvedtaksstotte, men vi har enda ikke implementert
         * støtte for den i våre systemer.
         * Hvis en bruker har innsatsgruppen GRADERT_VARIG_TILPASSET_INNSATS så har den samme
         * rettigheter som VARIG_TILPASSET_INNSATS.
         */
        GRADERT_VARIG_TILPASSET_INNSATS,
    }
}
