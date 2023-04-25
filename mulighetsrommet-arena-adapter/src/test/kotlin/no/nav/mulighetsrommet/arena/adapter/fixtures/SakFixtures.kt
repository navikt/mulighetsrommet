package no.nav.mulighetsrommet.arena.adapter.fixtures

import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaSak

object SakFixtures {
    val ArenaTiltakSak = ArenaSak(
        SAK_ID = 1,
        SAKSKODE = "TILT",
        AAR = 2022,
        LOPENRSAK = 1,
        AETATENHET_ANSVARLIG = "2990",
    )

    val ArenaIkkeTiltakSak = ArenaSak(
        SAK_ID = 1,
        SAKSKODE = "NOT_TILT",
        AAR = 2022,
        LOPENRSAK = 1,
        AETATENHET_ANSVARLIG = "2990",
    )
}
