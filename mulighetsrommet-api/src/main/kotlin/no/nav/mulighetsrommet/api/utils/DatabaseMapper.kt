package no.nav.mulighetsrommet.api.utils

import kotliquery.Row
import no.nav.mulighetsrommet.domain.Innsatsgruppe
import no.nav.mulighetsrommet.domain.Tiltaksgjennomforing
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype

object DatabaseMapper {

    fun toTiltakstype(row: Row): Tiltakstype =
        Tiltakstype(
            id = row.int("id"),
            navn = row.string("navn"),
            innsatsgruppe = row.int("innsatsgruppe_id"),
            sanityId = row.intOrNull("sanity_id"),
            tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
            fraDato = row.localDateTimeOrNull("fra_dato"),
            tilDato = row.localDateTimeOrNull("til_dato"),
        )

    fun toTiltaksgjennomforing(row: Row): Tiltaksgjennomforing =
        Tiltaksgjennomforing(
            id = row.int("id"),
            navn = row.string("navn"),
            tiltaksnummer = row.int("tiltaksnummer"),
            arrangorId = row.intOrNull("arrangor_id"),
            tiltakskode = Tiltakskode.valueOf(row.string("tiltakskode")),
            arenaId = row.int("arena_id"),
            sakId = row.int("sak_id"),
            sanityId = row.intOrNull("sanity_id"),
            fraDato = row.localDateTimeOrNull("fra_dato"),
            tilDato = row.localDateTimeOrNull("til_dato"),
        )

    fun toInnsatsgruppe(row: Row): Innsatsgruppe = Innsatsgruppe(
        id = row.int("id"),
        tittel = row.string("tittel"),
        beskrivelse = row.string("beskrivelse")
    )
}
