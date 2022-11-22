package no.nav.mulighetsrommet.api.utils

import kotliquery.Row
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltak
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltakdeltaker
import no.nav.mulighetsrommet.domain.adapter.AdapterTiltaksgjennomforing
import no.nav.mulighetsrommet.domain.models.*
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing.Tilgjengelighetsstatus

object DatabaseMapper {

    fun toTiltakstype(row: Row): Tiltakstype =
        Tiltakstype(
            id = row.uuid("id"),
            navn = row.string("navn"),
            tiltakskode = row.string("tiltakskode")
        )

    fun toTiltaksgjennomforing(row: Row): Tiltaksgjennomforing =
        Tiltaksgjennomforing(
            id = row.uuid("id"),
            navn = row.string("navn"),
            tiltakstypeId = row.uuid("tiltakstype_id"),
            tiltaksnummer = row.string("tiltaksnummer")
        )

    fun toDeltaker(row: Row): Deltaker = Deltaker(
        id = row.uuid("id"),
        tiltaksgjennomforingId = row.uuid("tiltaksgjennomforing_id"),
        fnr = row.string("fnr"),
        status = Deltakerstatus.valueOf(row.string("status"))
    )

    fun toBrukerHistorikk(row: Row): HistorikkForDeltaker = HistorikkForDeltaker(
        id = row.string("id"),
        fraDato = row.localDateTimeOrNull("fra_dato"),
        tilDato = row.localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(row.string("status")),
        tiltaksnavn = row.string("navn"),
        tiltaksnummer = row.string("tiltaksnummer"),
        tiltakstype = row.string("tiltakstype"),
        arrangorId = row.int("arrangor_id")
    )
}
