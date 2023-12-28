package no.nav.mulighetsrommet.arena.adapter.fixtures

import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus

object TiltaksgjennomforingFixtures {
    val ArenaTiltaksgjennomforingGruppe = ArenaTiltaksgjennomforing(
        TILTAKGJENNOMFORING_ID = 3780431,
        LOKALTNAVN = "Oppfølging",
        TILTAKSKODE = "INDOPPFAG",
        ARBGIV_ID_ARRANGOR = 49612,
        SAK_ID = 13572352,
        REG_DATO = "2022-10-10 00:00:00",
        DATO_FRA = "2022-10-10 00:00:00",
        DATO_TIL = null,
        STATUS_TREVERDIKODE_INNSOKNING = JaNeiStatus.Ja,
        ANTALL_DELTAKERE = 5,
        TILTAKSTATUSKODE = "GJENNOMFOR",
        AVTALE_ID = null,
        KLOKKETID_FREMMOTE = null,
        DATO_FREMMOTE = null,
        TEKST_KURSSTED = null,
        EKSTERN_ID = null,
    )

    val ArenaTiltaksgjennomforingIndividuell = ArenaTiltaksgjennomforing(
        TILTAKGJENNOMFORING_ID = 3780431,
        LOKALTNAVN = "AMO",
        TILTAKSKODE = "AMO",
        ARBGIV_ID_ARRANGOR = 49612,
        SAK_ID = 13572352,
        REG_DATO = "2022-10-10 00:00:00",
        DATO_FRA = "2022-10-10 00:00:00",
        DATO_TIL = null,
        STATUS_TREVERDIKODE_INNSOKNING = JaNeiStatus.Ja,
        ANTALL_DELTAKERE = 5,
        TILTAKSTATUSKODE = "GJENNOMFOR",
        AVTALE_ID = null,
        KLOKKETID_FREMMOTE = null,
        DATO_FREMMOTE = null,
        TEKST_KURSSTED = null,
        EKSTERN_ID = null,
    )
}
