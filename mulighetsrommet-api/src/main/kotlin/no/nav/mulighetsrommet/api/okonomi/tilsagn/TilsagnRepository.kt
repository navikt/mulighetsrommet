package no.nav.mulighetsrommet.api.okonomi.tilsagn

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDate
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
                beregning,
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
                :beregning::jsonb,
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
                beregning               = excluded.beregning,
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

    fun getAllArrangorflateTilsagn(organisasjonsnummer: Organisasjonsnummer): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where arrangor_organisasjonsnummer = :organisasjonsnummer
        """.trimIndent()

        return queryOf(query, mapOf("organisasjonsnummer" to organisasjonsnummer.value))
            .map { it.toArrangorflateTilsagn() }
            .asList
            .let { db.run(it) }
    }

    fun getArrangorflateTilsagnTilRefusjon(
        gjennomforingId: UUID,
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
    ): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where gjennomforing_id = :gjennomforing_id::uuid
            and (:periode_slutt::date is null or periode_start <= :periode_slutt::date)
            and (:periode_start::date is null or periode_slutt >= :periode_start::date)
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "gjennomforing_id" to gjennomforingId,
                "periode_start" to periodeStart,
                "periode_slutt" to periodeSlutt,
            ),
        )
            .map { it.toArrangorflateTilsagn() }
            .asList
            .let { db.run(it) }
    }

    fun getArrangorflateTilsagn(id: UUID): ArrangorflateTilsagn? {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toArrangorflateTilsagn() }
            .asSingle
            .let { db.run(it) }
    }

    fun setAnnullertTidspunkt(id: UUID, tidspunkt: LocalDateTime, tx: Session): Int {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set annullert_tidspunkt = :tidspunkt
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
        "beregning" to Json.encodeToString(beregning),
        "besluttelse" to null,
        "besluttet_tidspunkt" to null,
        "besluttet_av" to null,
    )

    private fun Row.toTilsagnDto(): TilsagnDto {
        return TilsagnDto(
            id = uuid("id"),
            tiltaksgjennomforing = TilsagnDto.Tiltaksgjennomforing(
                id = uuid("tiltaksgjennomforing_id"),
                antallPlasser = int("antall_plasser"),
                tiltaksnummer = string("tiltaksnummer"),
            ),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
            opprettetAv = NavIdent(string("opprettet_av")),
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
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            beregning = Json.decodeFromString<Prismodell.TilsagnBeregning>(string("beregning")),
        )
    }

    private fun Row.toArrangorflateTilsagn(): ArrangorflateTilsagn {
        return ArrangorflateTilsagn(
            id = uuid("id"),
            gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                navn = string("gjennomforing_navn"),
            ),
            tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                navn = string("tiltakstype_navn"),
            ),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
            arrangor = ArrangorflateTilsagn.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
            ),
            beregning = Json.decodeFromString<Prismodell.TilsagnBeregning>(string("beregning")),
        )
    }
}
