package no.nav.mulighetsrommet.api.tilsagn.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Prismodell
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
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
                kostnadssted,
                arrangor_id,
                status,
                type,
                belop_gjenstaende,
                belop_beregnet,
                prismodell
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :periode::daterange,
                :lopenummer,
                :bestillingsnummer,
                :kostnadssted,
                :arrangor_id::uuid,
                :status::tilsagn_status,
                :type::tilsagn_type,
                :belop_gjenstaende,
                :belop_beregnet,
                :prismodell::prismodell
            )
            on conflict (id) do update set
                gjennomforing_id        = excluded.gjennomforing_id,
                periode                 = excluded.periode,
                lopenummer              = excluded.lopenummer,
                bestillingsnummer       = excluded.bestillingsnummer,
                kostnadssted            = excluded.kostnadssted,
                arrangor_id             = excluded.arrangor_id,
                status                  = excluded.status,
                type                    = excluded.type,
                belop_gjenstaende       = excluded.belop_gjenstaende,
                belop_beregnet          = excluded.belop_beregnet,
                prismodell              = excluded.prismodell
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "periode" to dbo.periode.toDaterange(),
            "lopenummer" to dbo.lopenummer,
            "status" to TilsagnStatus.TIL_GODKJENNING.name,
            "bestillingsnummer" to dbo.bestillingsnummer,
            "kostnadssted" to dbo.kostnadssted,
            "arrangor_id" to dbo.arrangorId,
            "type" to dbo.type.name,
            "belop_gjenstaende" to dbo.beregning.output.belop,
            "belop_beregnet" to dbo.beregning.output.belop,
            "prismodell" to when (dbo.beregning) {
                is TilsagnBeregningForhandsgodkjent -> Prismodell.FORHANDSGODKJENT
                is TilsagnBeregningFri -> Prismodell.FRI
            }.name,
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

        TotrinnskontrollQueries(this).upsert(
            Totrinnskontroll(
                id = UUID.randomUUID(),
                entityId = dbo.id,
                behandletAv = dbo.behandletAv,
                aarsaker = emptyList(),
                forklaring = null,
                type = Totrinnskontroll.Type.OPPRETT,
                behandletTidspunkt = dbo.behandletTidspunkt,
                besluttelse = null,
                besluttetAv = null,
                besluttetTidspunkt = null,
            ),
        )
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

    fun setGjenstaendeBelop(id: UUID, belop: Int) = with(session) {
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

        execute(queryOf(query, params))
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

    fun get(id: UUID): TilsagnDto? {
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
        statuser: List<TilsagnStatus>? = null,
        periodeIntersectsWith: Periode? = null,
    ): List<TilsagnDto> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_admin_dto_view
            where
              (:typer::tilsagn_type[] is null or type = any(:typer::tilsagn_type[]))
              and (:gjennomforing_id::uuid is null or gjennomforing_id = :gjennomforing_id::uuid)
              and (:statuser::tilsagn_status[] is null or status::tilsagn_status = any(:statuser))
              and (:periode::daterange is null or periode && :periode::daterange)
            order by lopenummer desc
        """.trimIndent()

        val params = mapOf(
            "typer" to typer?.map { it.name }?.let { session.createArrayOf("tilsagn_type", it) },
            "gjennomforing_id" to gjennomforingId,
            "statuser" to statuser?.let { session.createArrayOf("tilsagn_status", statuser) },
            "periode" to periodeIntersectsWith?.toDaterange(),
        )

        return session.list(queryOf(query, params)) { it.toTilsagnDto() }
    }

    fun getAllArrangorflateTilsagn(organisasjonsnummer: Organisasjonsnummer): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_arrangorflate_view
            where arrangor_organisasjonsnummer = ?
            and status in ('GODKJENT', 'TIL_ANNULLERING', 'ANNULLERT')
        """.trimIndent()

        return session.list(queryOf(query, organisasjonsnummer.value)) { it.toArrangorflateTilsagn() }
    }

    fun getArrangorflateTilsagnTilUtbetaling(
        gjennomforingId: UUID,
        periodeIntersectsWith: Periode,
    ): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_arrangorflate_view
            where gjennomforing_id = :gjennomforing_id::uuid
              and (periode && :periode::daterange)
              and status in ('GODKJENT', 'TIL_ANNULLERING', 'ANNULLERT')
              and type in ('EKSTRATILSAGN', 'TILSAGN')
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to gjennomforingId,
            "periode" to periodeIntersectsWith.toDaterange(),
        )

        return session.list(queryOf(query, params)) { it.toArrangorflateTilsagn() }
    }

    fun getArrangorflateTilsagn(id: UUID): ArrangorflateTilsagn? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_arrangorflate_view
            where id = ?::uuid
            and status in ('GODKJENT', 'TIL_ANNULLERING', 'ANNULLERT')
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toArrangorflateTilsagn() }
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

    private fun Row.toTilsagnDto(): TilsagnDto {
        val id = uuid("id")

        val beregning = getBeregning(id, Prismodell.valueOf(string("prismodell")))

        val opprettelse = TotrinnskontrollQueries(session).get(id, Totrinnskontroll.Type.OPPRETT)
        val annullering = TotrinnskontrollQueries(session).get(id, Totrinnskontroll.Type.ANNULLER)
        val frigjoring = TotrinnskontrollQueries(session).get(id, Totrinnskontroll.Type.FRIGJOR)
        requireNotNull(opprettelse)

        return TilsagnDto(
            id = uuid("id"),
            type = TilsagnType.valueOf(string("type")),
            gjennomforing = TilsagnDto.Gjennomforing(
                id = uuid("gjennomforing_id"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
                navn = string("gjennomforing_navn"),
            ),
            belopGjenstaende = int("belop_gjenstaende"),
            periode = periode("periode"),
            lopenummer = int("lopenummer"),
            bestillingsnummer = string("bestillingsnummer"),
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
            beregning = beregning,
            status = TilsagnStatus.valueOf(string("status")),
            opprettelse = opprettelse,
            annullering = annullering,
            frigjoring = frigjoring,
        )
    }

    private fun Row.toArrangorflateTilsagn(): ArrangorflateTilsagn {
        val id = uuid("id")

        val beregning = getBeregning(id, Prismodell.valueOf(string("prismodell")))

        val opprettelse = TotrinnskontrollQueries(session).get(id, Totrinnskontroll.Type.OPPRETT)
        val annullering = TotrinnskontrollQueries(session).get(id, Totrinnskontroll.Type.ANNULLER)
        requireNotNull(opprettelse)

        return ArrangorflateTilsagn(
            id = uuid("id"),
            gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                navn = string("gjennomforing_navn"),
            ),
            gjenstaendeBelop = int("belop_gjenstaende"),
            tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                navn = string("tiltakstype_navn"),
            ),
            type = TilsagnType.valueOf(string("type")),
            periode = periode("periode"),
            arrangor = ArrangorflateTilsagn.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
            ),
            beregning = beregning,
            status = ArrangorflateTilsagn.StatusOgAarsaker(
                status = TilsagnStatus.valueOf(string("status")),
                aarsaker = annullering?.aarsaker?.map { TilsagnStatusAarsak.valueOf(it) } ?: emptyList(),
            ),
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
