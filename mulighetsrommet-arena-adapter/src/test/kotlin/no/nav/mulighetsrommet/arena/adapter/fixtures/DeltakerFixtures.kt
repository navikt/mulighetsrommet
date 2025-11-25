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
        MOD_DATO = "2023-01-02 00:00:00",
        PROSENT_DELTID = 100.0,
        ANTALL_DAGER_PR_UKE = 5.0,
    )

    val ArenaHistTiltakdeltaker = ArenaHistTiltakdeltaker(
        HIST_TILTAKDELTAKER_ID = 1,
        PERSON_ID = 2,
        TILTAKGJENNOMFORING_ID = 3,
        DELTAKERSTATUSKODE = ArenaTiltakdeltakerStatus.GJENNOMFORES,
        DATO_FRA = null,
        DATO_TIL = null,
        REG_DATO = "2023-01-01 00:00:00",
        MOD_DATO = "2023-01-02 00:00:00",
        PROSENT_DELTID = 50.0,
        ANTALL_DAGER_PR_UKE = 2.5,
    )
}
