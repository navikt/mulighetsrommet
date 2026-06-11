package no.nav.mulighetsrommet.api.avtale.db

import java.util.UUID

data class AvtaleDbo(
    val id: UUID,
    val detaljerDbo: DetaljerDbo,
    val personvernDbo: PersonvernDbo,
    val veilederinformasjonDbo: VeilederinformasjonDbo,
    val prismodeller: List<UUID>,
)
