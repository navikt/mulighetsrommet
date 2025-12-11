package no.nav.mulighetsrommet.api.gjennomforing.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import org.intellij.lang.annotations.Language
import java.util.*

class GjennomforingEnkeltplassQueries(private val session: Session) {
    fun getOrError(id: UUID): Enkeltplass {
        return checkNotNull(get(id)) { "Enkeltplass med id=$id finnes ikke" }
    }

    fun get(id: UUID): Enkeltplass? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_gjennomforing_enkeltplass
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toEnkeltplass() }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        tiltakstyper: List<UUID> = emptyList(),
    ): PaginatedResult<Enkeltplass> {
        @Language("PostgreSQL")
        val query = """
            select *, count(*) over () as total_count
            from view_gjennomforing_enkeltplass
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any(:tiltakstype_ids))
            order by id
            limit :limit
            offset :offset
        """.trimIndent()

        val parameters = mapOf(
            "tiltakstype_ids" to tiltakstyper.ifEmpty { null }?.let { session.createUuidArray(it) },
        )

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toEnkeltplass() }
            .runWithSession(session)
    }
}

private fun Row.toEnkeltplass(): Enkeltplass {
    val arena = stringOrNull("arena_tiltaksnummer")?.let { tiltaksnummer ->
        Enkeltplass.ArenaData(
            tiltaksnummer = Tiltaksnummer(tiltaksnummer),
            navn = stringOrNull("arena_navn"),
            startDato = localDateOrNull("arena_start_dato"),
            sluttDato = localDateOrNull("arena_slutt_dato"),
            status = stringOrNull("arena_status")?.let { GjennomforingStatusType.valueOf(it) },
            ansvarligNavEnhet = stringOrNull("arena_ansvarlig_enhet"),
        )
    }

    return Enkeltplass(
        id = uuid("id"),
        opprettetTidspunkt = instant("opprettet_tidspunkt"),
        oppdatertTidspunkt = instant("oppdatert_tidspunkt"),
        arrangor = Enkeltplass.Arrangor(
            id = uuid("arrangor_id"),
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = string("arrangor_navn"),
            slettet = boolean("arrangor_slettet"),
        ),
        tiltakstype = Enkeltplass.Tiltakstype(
            id = uuid("tiltakstype_id"),
            navn = string("tiltakstype_navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        ),
        arena = arena,
    )
}
