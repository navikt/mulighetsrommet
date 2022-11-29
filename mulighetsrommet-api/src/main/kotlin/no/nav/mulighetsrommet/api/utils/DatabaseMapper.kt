package no.nav.mulighetsrommet.api.utils

import kotliquery.Row
import no.nav.mulighetsrommet.domain.models.*

object DatabaseMapper {

    fun toTiltakstype(row: Row): Tiltakstype =
        Tiltakstype(
            id = row.uuid("id"),
            navn = row.string("navn"),
            tiltakskode = row.string("tiltakskode")
        )

    fun toDeltaker(row: Row): Deltaker = Deltaker(
        id = row.uuid("id"),
        tiltaksgjennomforingId = row.uuid("tiltaksgjennomforing_id"),
        norskIdent = row.string("fnr"),
        status = Deltakerstatus.valueOf(row.string("status")),
        fraDato = row.localDateTime("fraDato"),
        tilDato = row.localDateTimeOrNull("tilDato"),
    )

    fun toBrukerHistorikk(row: Row): HistorikkForDeltaker = HistorikkForDeltaker(
        id = row.uuid("id"),
        fraDato = row.localDateTimeOrNull("fra_dato"),
        tilDato = row.localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(row.string("status")),
        tiltaksnavn = row.string("navn"),
        tiltaksnummer = row.string("tiltaksnummer"),
        tiltakstype = row.string("tiltakstype"),
        virksomhetsnummer = row.string("virksomhetsnummer")
    )

    fun toDelMedBruker(row: Row): DelMedBruker = DelMedBruker(
        id = row.string("id"),
        bruker_fnr = row.string("bruker_fnr"),
        navident = row.string("navident"),
        tiltaksnummer = row.string("tiltaksnummer"),
        dialogId = row.string("dialogId"),
        created_at = row.localDateTime("created_at"),
        updated_at = row.localDateTime("updated_at"),
        created_by = row.string("created_by"),
        updated_by = row.string("updated_by")
    )
}
