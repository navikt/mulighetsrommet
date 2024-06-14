package no.nav.mulighetsrommet.arena.adapter.fixtures

import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaHistTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltaker
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltakdeltakerStatus

object DeltakerFixtures {
    val ArenaTiltakdeltaker = ArenaTiltakdeltaker(
        TILTAKDELTAKER_ID = 1,
        PERSON_ID = 2,
        TILTAKGJENNOMFORING_ID = 3,
        DELTAKERSTATUSKODE = ArenaTiltakdeltakerStatus.GJENNOMFORES,
        DATO_FRA = null,
        DATO_TIL = null,
        REG_DATO = "2023-01-01 00:00:00",
    )

    val ArenaHistTiltakdeltaker = ArenaHistTiltakdeltaker(
        HIST_TILTAKDELTAKER_ID = 1,
        PERSON_ID = 2,
        TILTAKGJENNOMFORING_ID = 3,
        DELTAKERSTATUSKODE = ArenaTiltakdeltakerStatus.GJENNOMFORES,
        DATO_FRA = null,
        DATO_TIL = null,
        REG_DATO = "2023-01-01 00:00:00",
    )
}
