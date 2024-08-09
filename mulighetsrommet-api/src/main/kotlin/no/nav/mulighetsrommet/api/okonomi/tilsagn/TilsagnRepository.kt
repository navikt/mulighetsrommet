package no.nav.mulighetsrommet.api.okonomi.tilsagn

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class TilsagnRepository(private val db: Database) {
    fun upsert(dbo: TilsagnDbo) =
        db.transaction { upsert(dbo, it) }

    fun upsert(dbo: TilsagnDbo, tx: Session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilsagn (
                id,
                tiltaksgjennomforing_id,
                periode_start,
                periode_slutt,
                kostnadssted,
                opprettet_av,
                arrangor_id,
                belop,
                besluttet_av,
                besluttet_tidspunkt,
                besluttelse
            ) values (
                :id::uuid,
                :tiltaksgjennomforing_id::uuid,
                :periode_start,
                :periode_slutt,
                :kostnadssted,
                :opprettet_av,
                :arrangor_id::uuid,
                :belop,
                :besluttet_av,
                :besluttet_tidspunkt,
                :besluttelse
            )
            on conflict (id) do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                periode_start           = excluded.periode_start,
                periode_slutt           = excluded.periode_slutt,
                kostnadssted            = excluded.kostnadssted,
                opprettet_av            = excluded.opprettet_av,
                arrangor_id             = excluded.arrangor_id,
                belop                   = excluded.belop,
                besluttelse             = excluded.besluttelse,
                besluttet_av            = excluded.besluttet_av,
                besluttet_tidspunkt     = excluded.besluttet_tidspunkt
            returning *
        """.trimIndent()

        tx.run(queryOf(query, dbo.toSqlParameters()).asExecute)
    }

    fun get(id: UUID) = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): TilsagnDto? {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_admin_dto_view
            where id = :id::uuid
        """.trimIndent()

        return tx.run(
            queryOf(query, mapOf("id" to id))
                .map { it.toTilsagnDto() }
                .asSingle,
        )
    }

    fun getByGjennomforingId(gjennomforingId: UUID): List<TilsagnDto> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_admin_dto_view
            where tiltaksgjennomforing_id = :gjennomforing_id::uuid
            order by lopenummer desc
        """.trimIndent()

        return queryOf(query, mapOf("gjennomforing_id" to gjennomforingId))
            .map { it.toTilsagnDto() }
            .asList
            .let { db.run(it) }
    }

    fun setAnnullertTidspunkt(id: UUID, tidspunkt: LocalDateTime) = db.transaction {
        setAnnullertTidspunkt(id, tidspunkt, it)
    }

    fun setAnnullertTidspunkt(id: UUID, tidspunkt: LocalDateTime, tx: Session): Int {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                annullert_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        return tx.run(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)).asUpdate)
    }

    fun setBesluttelse(
        id: UUID,
        besluttelse: TilsagnBesluttelse,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
    ): Int = db.transaction { tx ->
        setBesluttelse(
            id,
            besluttelse,
            navIdent,
            tidspunkt,
            tx,
        )
    }

    fun setBesluttelse(
        id: UUID,
        besluttelse: TilsagnBesluttelse,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
        tx: Session,
    ): Int {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                besluttelse = :besluttelse::tilsagn_besluttelse,
                besluttet_av = :nav_ident,
                besluttet_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        return tx.run(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "besluttelse" to besluttelse.name,
                    "nav_ident" to navIdent.value,
                    "tidspunkt" to tidspunkt,
                ),
            ).asUpdate,
        )
    }

    private fun TilsagnDbo.toSqlParameters() = mapOf(
        "id" to id,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "periode_start" to periodeStart,
        "periode_slutt" to periodeSlutt,
        "kostnadssted" to kostnadssted,
        "opprettet_av" to opprettetAv.value,
        "arrangor_id" to arrangorId,
        "belop" to belop,
        "besluttelse" to null,
        "besluttet_tidspunkt" to null,
        "besluttet_av" to null,
    )

    private fun Row.toTilsagnDto(): TilsagnDto {
        return TilsagnDto(
            id = uuid("id"),
            tiltaksgjennomforingId = uuid("tiltaksgjennomforing_id"),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
            opprettetAv = NavIdent(string("opprettet_av")),
            belop = int("belop"),
            besluttelse = stringOrNull("besluttelse")?.let {
                TilsagnDto.Besluttelse(
                    navIdent = NavIdent(string("besluttet_av")),
                    utfall = TilsagnBesluttelse.valueOf(it),
                    tidspunkt = localDateTime("besluttet_tidspunkt"),
                )
            },
            annullertTidspunkt = localDateTimeOrNull("annullert_tidspunkt"),
            lopenummer = int("lopenummer"),
            kostnadssted = NavEnhetDbo(
                enhetsnummer = string("kostnadssted"),
                navn = string("kostnadssted_navn"),
                type = Norg2Type.valueOf(string("kostnadssted_type")),
                overordnetEnhet = stringOrNull("kostnadssted_overordnet_enhet"),
                status = NavEnhetStatus.valueOf(string("kostnadssted_status")),
            ),
            arrangor = TilsagnDto.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = string("arrangor_organisasjonsnummer"),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
        )
    }
}
