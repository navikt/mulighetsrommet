package no.nav.mulighetsrommet.api.tilsagn.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.*

class TilsagnQueries(private val session: Session) {
    fun upsert(dbo: TilsagnDbo) = withTransaction(session) {
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
                beregning,
                type
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                daterange(:periode_start, :periode_slutt),
                :lopenummer,
                :bestillingsnummer,
                :kostnadssted,
                :arrangor_id::uuid,
                :beregning::jsonb,
                :type::tilsagn_type
            )
            on conflict (id) do update set
                gjennomforing_id        = excluded.gjennomforing_id,
                periode                 = excluded.periode,
                lopenummer              = excluded.lopenummer,
                bestillingsnummer       = excluded.bestillingsnummer,
                kostnadssted            = excluded.kostnadssted,
                arrangor_id             = excluded.arrangor_id,
                beregning               = excluded.beregning,
                type                    = excluded.type
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "periode_start" to dbo.periode.start,
            "periode_slutt" to dbo.periode.slutt,
            "lopenummer" to dbo.lopenummer,
            "bestillingsnummer" to dbo.bestillingsnummer,
            "kostnadssted" to dbo.kostnadssted,
            "arrangor_id" to dbo.arrangorId,
            "beregning" to Json.encodeToString<TilsagnBeregning>(dbo.beregning),
            "type" to dbo.type.name,
        )

        execute(queryOf(query, params))

        TotrinnskontrollQueries(this).behandler(
            entityId = dbo.id,
            navIdent = dbo.endretAv,
            aarsaker = null,
            forklaring = null,
            type = TotrinnskontrollType.OPPRETT,
            tidspunkt = dbo.endretTidspunkt,
        )
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
        type: TilsagnType? = null,
        gjennomforingId: UUID? = null,
        statuser: List<TilsagnStatus>? = null,
    ): List<TilsagnDto> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_admin_dto_view
            where (:type::tilsagn_type is null or type = :type::tilsagn_type)
              and (:gjennomforing_id::uuid is null or gjennomforing_id = :gjennomforing_id::uuid)
              and (:statuser::tilsagn_status[] is null or status::tilsagn_status = any(:statuser))
            order by lopenummer desc
        """.trimIndent()

        val params = mapOf(
            "type" to type?.name,
            "gjennomforing_id" to gjennomforingId,
            "statuser" to statuser?.let { session.createArrayOf("tilsagn_status", statuser) },
        )

        return session.list(queryOf(query, params)) { it.toTilsagnDto() }
    }

    fun getAllArrangorflateTilsagn(organisasjonsnummer: Organisasjonsnummer): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where arrangor_organisasjonsnummer = ?
            and status in ('GODKJENT', 'TIL_ANNULLERING', 'ANNULLERT')
        """.trimIndent()

        return session.list(queryOf(query, organisasjonsnummer.value)) { it.toArrangorflateTilsagn() }
    }

    fun getTilsagnForGjennomforing(
        gjennomforingId: UUID,
        periode: Periode,
    ): List<TilsagnDto> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_admin_dto_view
            where gjennomforing_id = :gjennomforing_id::uuid
              and (periode_start <= :periode_slutt::date)
              and (periode_slutt >= :periode_start::date)
              and status in ('RETURNERT', 'TIL_GODKJENNING', 'GODKJENT')
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to gjennomforingId,
            "periode_start" to periode.start,
            "periode_slutt" to periode.getLastDate(),
        )

        return session.list(queryOf(query, params)) { it.toTilsagnDto() }
    }

    fun getArrangorflateTilsagnTilUtbetaling(
        gjennomforingId: UUID,
        periode: Periode,
    ): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where gjennomforing_id = :gjennomforing_id::uuid
              and (periode_start <= :periode_slutt::date)
              and (periode_slutt >= :periode_start::date)
              and status in ('GODKJENT', 'TIL_ANNULLERING', 'ANNULLERT')
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to gjennomforingId,
            "periode_start" to periode.start,
            "periode_slutt" to periode.getLastDate(),
        )

        return session.list(queryOf(query, params)) { it.toArrangorflateTilsagn() }
    }

    fun getArrangorflateTilsagn(id: UUID): ArrangorflateTilsagn? {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
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

    private fun Row.toTilsagnDto(): TilsagnDto {
        val id = uuid("id")
        val opprettelse = TotrinnskontrollQueries(session).get(id, TotrinnskontrollType.OPPRETT)
        val annullering = TotrinnskontrollQueries(session).get(id, TotrinnskontrollType.ANNULLER)
        requireNotNull(opprettelse)

        return TilsagnDto(
            id = uuid("id"),
            type = TilsagnType.valueOf(string("type")),
            gjennomforing = TilsagnDto.Gjennomforing(
                id = uuid("gjennomforing_id"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
                navn = string("gjennomforing_navn"),
            ),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
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
            beregning = Json.decodeFromString<TilsagnBeregning>(string("beregning")),
            status = TilsagnStatus.valueOf(string("status")),
            opprettelse = opprettelse,
            annullering = annullering,
        )
    }

    private fun Row.toArrangorflateTilsagn(): ArrangorflateTilsagn {
        val id = uuid("id")
        val aarsaker = TotrinnskontrollQueries(session).get(id, TotrinnskontrollType.ANNULLER)
            ?.aarsaker
            ?.map { TilsagnStatusAarsak.valueOf(it) } ?: emptyList()

        return ArrangorflateTilsagn(
            id = uuid("id"),
            gjennomforing = ArrangorflateTilsagn.Gjennomforing(
                navn = string("gjennomforing_navn"),
            ),
            tiltakstype = ArrangorflateTilsagn.Tiltakstype(
                navn = string("tiltakstype_navn"),
            ),
            type = TilsagnType.valueOf(string("type")),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
            arrangor = ArrangorflateTilsagn.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
            ),
            beregning = Json.decodeFromString<TilsagnBeregning>(string("beregning")),
            status = ArrangorflateTilsagn.StatusOgAarsaker(
                status = TilsagnStatus.valueOf(string("status")),
                aarsaker = aarsaker,
            ),
        )
    }
}
