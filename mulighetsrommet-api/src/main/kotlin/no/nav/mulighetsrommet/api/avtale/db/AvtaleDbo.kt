package no.nav.mulighetsrommet.api.avtale.db

import PersonvernDbo
import java.util.UUID

data class AvtaleDbo(
    val id: UUID,
    val detaljerDbo: DetaljerDbo,
    val personvernDbo: PersonvernDbo,
    val veilederinformasjonDbo: VeilederinformasjonDbo,
    val prismodellDbo: PrismodellDbo,
)
