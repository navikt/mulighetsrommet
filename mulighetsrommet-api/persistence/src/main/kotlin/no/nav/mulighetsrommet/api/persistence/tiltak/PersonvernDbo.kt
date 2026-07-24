package no.nav.mulighetsrommet.api.persistence.tiltak

import no.nav.mulighetsrommet.model.Personopplysning

data class PersonvernDbo(
    val personopplysninger: List<Personopplysning.Type>,
    val annetChecked: Boolean,
    val annetBeskrivelse: String?,
    val personvernBekreftet: Boolean,
)
