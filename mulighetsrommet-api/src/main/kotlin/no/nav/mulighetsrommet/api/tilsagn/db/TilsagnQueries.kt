package no.nav.mulighetsrommet.api.tilsagn.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.BestillingStatusType
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.*

class TilsagnQueries(private val session: Session) {
    fun upsert(dbo: TilsagnDbo): Unit = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilsagn (
                id,
                gjennomforing_id,
                periode,
                lopenummer,
                bestillingsnummer,
                bestilling_status,
                kostnadssted,
                status,
                type,
                belop_gjenstaende,
                belop_beregnet,
                prismodell,
                datastream_periode_start,
                datastream_periode_slutt
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :periode::daterange,
                :lopenummer,
                :bestillingsnummer,
                :bestilling_status,
                :kostnadssted,
                :status::tilsagn_status,
                :type::tilsagn_type,
                :belop_gjenstaende,
                :belop_beregnet,
                :prismodell::prismodell,
                :datastream_periode_start,
                :datastream_periode_slutt
            )
            on conflict (id) do update set
                gjennomforing_id        = excluded.gjennomforing_id,
                periode                 = excluded.periode,
                lopenummer              = excluded.lopenummer,
                bestillingsnummer       = excluded.bestillingsnummer,
                bestilling_status       = excluded.bestilling_status,
                kostnadssted            = excluded.kostnadssted,
                status                  = excluded.status,
                type                    = excluded.type,
                belop_gjenstaende       = excluded.belop_gjenstaende,
                belop_beregnet          = excluded.belop_beregnet,
                prismodell              = excluded.prismodell,
                datastream_periode_start = excluded.datastream_periode_start,
                datastream_periode_slutt = excluded.datastream_periode_slutt
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "periode" to dbo.periode.toDaterange(),
            "lopenummer" to dbo.lopenummer,
            "status" to TilsagnStatus.TIL_GODKJENNING.name,
            "bestillingsnummer" to dbo.bestillingsnummer,
            "bestilling_status" to dbo.bestillingStatus?.name,
            "kostnadssted" to dbo.kostnadssted.value,
            "type" to dbo.type.name,
            "belop_gjenstaende" to dbo.beregning.output.belop,
            "belop_beregnet" to dbo.beregning.output.belop,
            "prismodell" to when (dbo.beregning) {
                is TilsagnBeregningForhandsgodkjent -> Prismodell.FORHANDSGODKJENT
                is TilsagnBeregningFri -> Prismodell.FRI
            }.name,
            "datastream_periode_start" to dbo.periode.start,
            "datastream_periode_slutt" to dbo.periode.getLastInclusiveDate(),
        )

        execute(queryOf(query, params))

        when (dbo.beregning) {
            is TilsagnBeregningForhandsgodkjent -> {
                check(dbo.periode == dbo.beregning.input.periode) {
                    "Tilsagnsperiode og beregningsperiode må være lik"
                }
                upsertTilsagnBeregningForhandsgodkjent(dbo.id, dbo.beregning)
            }

            is TilsagnBeregningFri -> {}
        }
    }

    private fun TransactionalSession.upsertTilsagnBeregningForhandsgodkjent(
        id: UUID,
        beregning: TilsagnBeregningForhandsgodkjent,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into tilsagn_forhandsgodkjent_beregning (
                tilsagn_id,
                sats,
                antall_plasser
            ) values (
                :tilsagn_id::uuid,
                :sats,
                :antall_plasser
            )
            on conflict (tilsagn_id) do update set
                sats = excluded.sats,
                antall_plasser = excluded.antall_plasser
        """.trimIndent()

        val params = mapOf(
            "tilsagn_id" to id,
            "sats" to beregning.input.sats,
            "antall_plasser" to beregning.input.antallPlasser,
        )

        execute(queryOf(query, params))
    }

    fun setGjenstaendeBelop(id: UUID, belop: Int) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                belop_gjenstaende = :belop
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "belop" to belop,
        )

        session.execute(queryOf(query, params))
    }

    fun getNextLopenummeByGjennomforing(gjennomforingId: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            select coalesce(max(lopenummer), 0) + 1 as lopenummer
            from tilsagn
            where gjennomforing_id = ?
        """.trimIndent()

        return session.requireSingle(queryOf(query, gjennomforingId)) { it.int("lopenummer") }
    }

    fun get(id: UUID): Tilsagn? {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_admin_dto_view
            where id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("id" to id))) { it.toTilsagnDto() }
    }

    fun getAll(
        typer: List<TilsagnType>? = null,
        gjennomforingId: UUID? = null,
        arrangor: Organisasjonsnummer? = null,
        statuser: List<TilsagnStatus>? = null,
        periodeIntersectsWith: Periode? = null,
    ): List<Tilsagn> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_admin_dto_view
            where
              (:typer::tilsagn_type[] is null or type = any(:typer::tilsagn_type[]))
              and (:gjennomforing_id::uuid is null or gjennomforing_id = :gjennomforing_id::uuid)
              and (:arrangor::text is null or arrangor_organisasjonsnummer = :arrangor::text)
              and (:statuser::tilsagn_status[] is null or status::tilsagn_status = any(:statuser))
              and (:periode::daterange is null or periode && :periode::daterange)
            order by lopenummer desc
        """.trimIndent()

        val params = mapOf(
            "typer" to typer?.let { session.createArrayOfTilsagnType(it) },
            "gjennomforing_id" to gjennomforingId,
            "arrangor" to arrangor?.value,
            "statuser" to statuser?.let { session.createArrayOfTilsagnStatus(it) },
            "periode" to periodeIntersectsWith?.toDaterange(),
        )

        return session.list(queryOf(query, params)) { it.toTilsagnDto() }
    }

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from tilsagn where id = ?
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun setStatus(id: UUID, status: TilsagnStatus) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = :status::tilsagn_status where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id, "status" to status.name)))
    }

    fun setBestillingStatus(bestillingsnummer: String, status: BestillingStatusType) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set bestilling_status = ? where bestillingsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, status.name, bestillingsnummer))
    }

    private fun Row.toTilsagnDto(): Tilsagn {
        val id = uuid("id")

        val beregning = getBeregning(id, Prismodell.valueOf(string("prismodell")))

        return Tilsagn(
            id = uuid("id"),
            type = TilsagnType.valueOf(string("type")),
            tiltakstype = Tilsagn.Tiltakstype(
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
                navn = string("tiltakstype_navn"),
            ),
            gjennomforing = Tilsagn.Gjennomforing(
                id = uuid("gjennomforing_id"),
                navn = string("gjennomforing_navn"),
            ),
            belopGjenstaende = int("belop_gjenstaende"),
            periode = periode("periode"),
            lopenummer = int("lopenummer"),
            bestilling = Tilsagn.Bestilling(
                bestillingsnummer = string("bestillingsnummer"),
                status = stringOrNull("bestilling_status")?.let { BestillingStatusType.valueOf(it) },
            ),
            kostnadssted = NavEnhetDbo(
                enhetsnummer = NavEnhetNummer(string("kostnadssted")),
                navn = string("kostnadssted_navn"),
                type = Norg2Type.valueOf(string("kostnadssted_type")),
                overordnetEnhet = stringOrNull("kostnadssted_overordnet_enhet")?.let { NavEnhetNummer(it) },
                status = NavEnhetStatus.valueOf(string("kostnadssted_status")),
            ),
            arrangor = Tilsagn.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            beregning = beregning,
            status = TilsagnStatus.valueOf(string("status")),
        )
    }

    private fun getBeregning(id: UUID, prismodell: Prismodell): TilsagnBeregning {
        return when (prismodell) {
            Prismodell.FORHANDSGODKJENT -> getBeregningForhandsgodkjent(id)
            Prismodell.FRI -> getBeregningFri(id)
        }
    }

    private fun getBeregningForhandsgodkjent(id: UUID): TilsagnBeregningForhandsgodkjent {
        @Language("PostgreSQL")
        val query = """
            select tilsagn.periode, tilsagn.belop_beregnet, beregning.sats, beregning.antall_plasser
            from tilsagn join tilsagn_forhandsgodkjent_beregning beregning on tilsagn.id = beregning.tilsagn_id
            where tilsagn.id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { row ->
            TilsagnBeregningForhandsgodkjent(
                input = TilsagnBeregningForhandsgodkjent.Input(
                    periode = row.periode("periode"),
                    sats = row.int("sats"),
                    antallPlasser = row.int("antall_plasser"),
                ),
                output = TilsagnBeregningForhandsgodkjent.Output(
                    belop = row.int("belop_beregnet"),
                ),
            )
        }
    }

    private fun getBeregningFri(id: UUID): TilsagnBeregningFri {
        @Language("PostgreSQL")
        val query = """
            select belop_beregnet
            from tilsagn
            where id = ?::uuid
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) {
            TilsagnBeregningFri(
                input = TilsagnBeregningFri.Input(
                    belop = it.int("belop_beregnet"),
                ),
                output = TilsagnBeregningFri.Output(
                    belop = it.int("belop_beregnet"),
                ),
            )
        }
    }
}

fun Session.createArrayOfTilsagnType(
    types: List<TilsagnType>,
): Array = createArrayOf("tilsagn_type", types)

fun Session.createArrayOfTilsagnStatus(
    statuser: List<TilsagnStatus>,
): Array = createArrayOf("tilsagn_status", statuser)
