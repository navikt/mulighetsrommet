package no.nav.mulighetsrommet.api.okonomi.tilsagn

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.database.Database
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
                belop
            ) values (
                :id::uuid,
                :tiltaksgjennomforing_id::uuid,
                :periode_start,
                :periode_slutt,
                :kostnadssted,
                :opprettet_av,
                :arrangor_id::uuid,
                :belop
            )
            on conflict (id) do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                periode_start           = excluded.periode_start,
                periode_slutt           = excluded.periode_slutt,
                kostnadssted            = excluded.kostnadssted,
                opprettet_av            = excluded.opprettet_av,
                arrangor_id             = excluded.arrangor_id,
                belop                   = excluded.belop
            returning *
        """.trimIndent()

        tx.run(queryOf(query, dbo.toSqlParameters()).asExecute)
    }

    fun get(id: UUID) = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): TilsagnDto? {
        @Language("PostgreSQL")
        val query = """
            select
                tilsagn.id,
                tilsagn.tiltaksgjennomforing_id,
                tilsagn.periode_start,
                tilsagn.periode_slutt,
                tilsagn.belop,
                tilsagn.sendt_tidspunkt,
                tilsagn.annullert_tidspunkt,
                tilsagn.lopenummer,
                tilsagn.kostnadssted,
                nav_enhet.navn              as kostnadssted_navn,
                nav_enhet.overordnet_enhet  as kostnadssted_overordnet_enhet,
                nav_enhet.type              as kostnadssted_type,
                nav_enhet.status            as kostnadssted_status,
                arrangor.id                         as arrangor_id,
                arrangor.organisasjonsnummer        as arrangor_organisasjonsnummer,
                arrangor.navn                       as arrangor_navn,
                arrangor.slettet_dato is not null   as arrangor_slettet
            from tilsagn
                inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
                inner join arrangor on arrangor.id = tilsagn.arrangor_id
            where tilsagn.id = :id::uuid
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
            select * from tilsagn
            where tiltaksgjennomforing_id = :gjennomforing_id::uuid
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
                annulert_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        return tx.run(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)).asUpdate)
    }

    fun setSendtTidspunkt(id: UUID, tidspunkt: LocalDateTime) = db.transaction {
        setSendtTidspunkt(id, tidspunkt, it)
    }

    fun setSendtTidspunkt(id: UUID, tidspunkt: LocalDateTime, tx: Session): Int {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                sendt_tidspunkt = :tidspunkt
            where id = :id::uuid
        """.trimIndent()

        return tx.run(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt)).asUpdate)
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
    )

    private fun Row.toTilsagnDto(): TilsagnDto {
        return TilsagnDto(
            id = uuid("id"),
            tiltaksgjennomforingId = uuid("tiltaksgjennomforing_id"),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
            belop = int("belop"),
            sendtTidspunkt = localDateTimeOrNull("sendt_tidspunkt"),
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
