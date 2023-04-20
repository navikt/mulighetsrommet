package no.nav.mulighetsrommet.arena.adapter.fixtures

import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaAvtaleInfo
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalekode
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalestatuskode

object AvtaleFixtures {
    val ArenaAvtaleInfo = ArenaAvtaleInfo(
        AVTALE_ID = 1000,
        AAR = 2022,
        LOPENRAVTALE = 2000,
        AVTALENAVN = "Avtale",
        ARKIVREF = "websak",
        ARBGIV_ID_LEVERANDOR = 1,
        PRIS_BETBETINGELSER = "Over 9000",
        DATO_FRA = "2022-01-04 00:00:00",
        DATO_TIL = "2023-03-04 00:00:00",
        TILTAKSKODE = "INDOPPFAG",
        ORGENHET_ANSVARLIG = "2990",
        BRUKER_ID_ANSVARLIG = "SIAMO",
        TEKST_ANDREOPPL = null,
        TEKST_FAGINNHOLD = "Faginnhold",
        TEKST_MAALGRUPPE = "Alle sammen",
        AVTALEKODE = Avtalekode.Rammeavtale,
        AVTALESTATUSKODE = Avtalestatuskode.Gjennomforer,
        STATUS_DATO_ENDRET = "2023-01-05 00:00:00",
        REG_DATO = "2022-10-04 00:00:00",
        REG_USER = "SIAMO",
        MOD_DATO = "2022-10-05 00:00:00",
        MOD_USER = "SIAMO",
        PROFILELEMENT_ID_OPPL_TILTAK = 3000,
    )
}
