package no.nav.mulighetsrommet.api.arrangorflate.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.api.avtale.db.toPrismodell
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.intellij.lang.annotations.Language
import java.util.UUID

class ArrangorflateTiltakQueries(private val session: Session) {

    fun getOrError(id: UUID): ArrangorflateTiltak {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_arrangorflate_tiltak
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.toArrangorflateTiltak() }
    }

    fun getAll(
        tiltakstyper: List<UUID>,
        organisasjonsnummer: List<Organisasjonsnummer>,
        prismodeller: List<PrismodellType>,
        filter: ArrangorflateTiltakFilter,
    ): PaginatedResult<ArrangorflateTiltak> = with(session) {
        val direction = when (filter.direction) {
            ArrangorflateFilterDirection.ASC -> "asc"
            ArrangorflateFilterDirection.DESC -> "desc"
        }

        val order = when (filter.orderBy) {
            ArrangorflateTiltakFilter.OrderBy.TILTAK -> "tiltakstype_navn $direction, navn $direction"
            ArrangorflateTiltakFilter.OrderBy.ARRANGOR -> "arrangor_navn $direction, arrangor_organisasjonsnummer $direction"
            ArrangorflateTiltakFilter.OrderBy.START_DATO -> "start_dato $direction"
            ArrangorflateTiltakFilter.OrderBy.SLUTT_DATO -> "slutt_dato $direction"
            ArrangorflateTiltakFilter.OrderBy.STATUS -> "status $direction"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from view_arrangorflate_tiltak
            where
              (:sok::text is null
                or arrangor_navn ilike :sok
                or arrangor_organisasjonsnummer ilike :sok
                or tiltakstype_navn ilike :sok
                or navn ilike :sok
                or lopenummer ilike :sok
                or to_char(start_dato, 'DD.MM.YYYY') ilike :sok
                or to_char(slutt_dato, 'DD.MM.YYYY') ilike :sok
              )
              and (:slutt_dato_cutoff::date is null or slutt_dato >= :slutt_dato_cutoff or slutt_dato is null)
              and tiltakstype_id = any(:tiltakstype_ids)
              and arrangor_organisasjonsnummer = any(:arrangor_orgnrs)
              and status = any(:statuser)
              and prismodell_type = any(:prismodeller::prismodell_type[])
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val params = mapOf(
            "sok" to filter.sok?.let { "%$it%" },
            "slutt_dato_cutoff" to filter.sluttDatoGreaterThanOrEqualTo,
            "tiltakstype_ids" to createUuidArray(tiltakstyper),
            "arrangor_orgnrs" to createArrayOfValue(organisasjonsnummer) { it.value },
            "statuser" to createArrayOf("gjennomforing_status", filter.type.toGjennomforingStatuses()),
            "prismodeller" to createArrayOf("prismodell_type", prismodeller),
        )

        return queryOf(query, params + filter.pagination.parameters)
            .mapPaginated { it.toArrangorflateTiltak() }
            .runWithSession(session)
    }
}

private fun Row.toArrangorflateTiltak(): ArrangorflateTiltak {
    val tiltakstype = ArrangorflateTiltak.Tiltakstype(
        id = uuid("tiltakstype_id"),
        navn = string("tiltakstype_navn"),
        tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
    )
    val arrangor = ArrangorflateTiltak.ArrangorUnderenhet(
        id = uuid("arrangor_id"),
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        navn = string("arrangor_navn"),
    )
    return ArrangorflateTiltak(
        id = uuid("id"),
        navn = string("navn"),
        lopenummer = Tiltaksnummer(string("lopenummer")),
        startDato = localDate("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = GjennomforingStatusType.GJENNOMFORES,
        arrangor = arrangor,
        tiltakstype = tiltakstype,
        prismodell = toPrismodell(),
    )
}
