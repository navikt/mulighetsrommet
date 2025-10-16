package no.nav.mulighetsrommet.api.gjennomforing.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class EnkeltplassQueries(private val session: Session) {
    fun upsert(dbo: EnkeltplassDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into enkeltplass (
                id,
                tiltakstype_id,
                arrangor_id
            )
            values (
                :id::uuid,
                :tiltakstype_id::uuid,
                :arrangor_id::uuid
            )
            on conflict (id) do update set
                tiltakstype_id = excluded.tiltakstype_id,
                arrangor_id    = excluded.arrangor_id;
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "tiltakstype_id" to dbo.tiltakstypeId,
            "arrangor_id" to dbo.arrangorId,
        )
        session.execute(queryOf(query, params))
    }

    fun setArenaData(dbo: EnkeltplassArenaDataDbo) {
        @Language("PostgreSQL")
        val query = """
                update enkeltplass
                set arena_tiltaksnummer = :arena_tiltaksnummer,
                    arena_navn = :arena_navn,
                    arena_start_dato = :arena_start_dato,
                    arena_slutt_dato = :arena_slutt_dato,
                    arena_status = :arena_status::gjennomforing_status,
                    arena_ansvarlig_enhet = :arena_ansvarlig_enhet
                where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "arena_tiltaksnummer" to dbo.tiltaksnummer,
            "arena_navn" to dbo.navn,
            "arena_start_dato" to dbo.startDato,
            "arena_slutt_dato" to dbo.sluttDato,
            "arena_status" to dbo.status?.name,
            "arena_ansvarlig_enhet" to dbo.arenaAnsvarligEnhet,
        )
        session.execute(queryOf(query, params))
    }

    fun getOrError(id: UUID): Enkeltplass {
        return checkNotNull(get(id)) { "Enkeltplass med id=$id finnes ikke" }
    }

    fun get(id: UUID): Enkeltplass? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_gjennomforing_enkeltplass_admin
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toEnkeltplass() }
    }

    fun getAll(
        tiltakstypeIder: List<UUID> = emptyList(),
    ): List<Enkeltplass> = with(session) {
        val parameters = mapOf(
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { createUuidArray(it) },
        )

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over () as total_count
            from gjennomforing_admin_dto_view
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any(:tiltakstype_ids))
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toEnkeltplass() }
    }

    fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete
            from enkeltplass
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }
}

private fun Row.toEnkeltplass(): Enkeltplass {
    val arena = stringOrNull("arena_tiltaksnummer")?.let { tiltaksnummer ->
        Enkeltplass.ArenaData(
            tiltaksnummer = tiltaksnummer,
            navn = stringOrNull("arena_navn"),
            startDato = localDateOrNull("arena_start_dato"),
            sluttDato = localDateOrNull("arena_slutt_dato"),
            status = stringOrNull("arena_status")?.let { GjennomforingStatusType.valueOf(it) },
            arenaAnsvarligEnhet = stringOrNull("arena_ansvarlig_enhet"),
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
