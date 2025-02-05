package no.nav.mulighetsrommet.api.tilsagn.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.ToTrinnskontrollQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.model.ToTrinnskontrollHandlingDto
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavIdent
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
                periode_start,
                periode_slutt,
                kostnadssted,
                arrangor_id,
                beregning,
                status,
                type
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :periode_start,
                :periode_slutt,
                :kostnadssted,
                :arrangor_id::uuid,
                :beregning::jsonb,
                'TIL_GODKJENNING'::tilsagn_status,
                :type::tilsagn_type
            )
            on conflict (id) do update set
                gjennomforing_id = excluded.gjennomforing_id,
                periode_start           = excluded.periode_start,
                periode_slutt           = excluded.periode_slutt,
                kostnadssted            = excluded.kostnadssted,
                arrangor_id             = excluded.arrangor_id,
                beregning               = excluded.beregning,
                status                  = excluded.status,
                type                    = excluded.type
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "gjennomforing_id" to dbo.gjennomforingId,
            "periode_start" to dbo.periodeStart,
            "periode_slutt" to dbo.periodeSlutt,
            "kostnadssted" to dbo.kostnadssted,
            "arrangor_id" to dbo.arrangorId,
            "beregning" to Json.encodeToString<TilsagnBeregning>(dbo.beregning),
            "status_endret_av" to dbo.endretAv.value,
            "status_endret_tidspunkt" to dbo.endretTidspunkt,
            "type" to dbo.type.name,
        )

        session.execute(queryOf(query, params))

        ToTrinnskontrollQueries(this).foreslaOpprett(
            entityId = dbo.id,
            opprettetAv = dbo.endretAv,
            aarsaker = emptyList(),
            forklaring = null,
        )
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
              and (:statuser::tilsagn_status[] is null or status = any(:statuser))
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
        """.trimIndent()

        return session.list(queryOf(query, organisasjonsnummer.value)) { it.toArrangorflateTilsagn() }
    }

    fun getTilsagnTilRefusjon(
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
            "periode_slutt" to periode.slutt,
        )

        return session.list(queryOf(query, params)) { it.toTilsagnDto() }
    }

    fun getArrangorflateTilsagnTilRefusjon(
        gjennomforingId: UUID,
        periode: Periode,
    ): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where gjennomforing_id = :gjennomforing_id::uuid
              and (periode_start <= :periode_slutt::date)
              and (periode_slutt >= :periode_start::date)
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to gjennomforingId,
            "periode_start" to periode.start,
            "periode_slutt" to periode.slutt,
        )

        return session.list(queryOf(query, params)) { it.toArrangorflateTilsagn() }
    }

    fun getArrangorflateTilsagn(id: UUID): ArrangorflateTilsagn? {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where id = ?::uuid
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

    fun godkjenn(
        id: UUID,
        navIdent: NavIdent,
    ) = withTransaction(session) {
        ToTrinnskontrollQueries(this).godkjenn(
            entityId = id,
            opprettetAv = navIdent,
            aarsaker = emptyList(),
            forklaring = null,
        )

        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = 'GODKJENT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id)))
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ) = withTransaction(session) {
        ToTrinnskontrollQueries(this).avvis(
            entityId = id,
            opprettetAv = navIdent,
            aarsaker = aarsaker.map { it.name },
            forklaring = forklaring,
        )

        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = 'RETURNERT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id)))
    }

    fun tilAnnullering(
        id: UUID,
        navIdent: NavIdent,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
    ) = withTransaction(session) {
        ToTrinnskontrollQueries(session).foreslaAnnuller(
            entityId = id,
            opprettetAv = navIdent,
            aarsaker = aarsaker.map { it.name },
            forklaring = forklaring,
        )

        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = 'TIL_ANNULLERING'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id)))
    }

    fun godkjennAnnullering(
        id: UUID,
        navIdent: NavIdent,
    ) {
        ToTrinnskontrollQueries(session).godkjenn(
            entityId = id,
            opprettetAv = navIdent,
            aarsaker = emptyList(),
            forklaring = null,
        )

        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = 'ANNULLERT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id)))
    }

    fun avvisAnnullering(
        id: UUID,
        navIdent: NavIdent,
    ) {
        ToTrinnskontrollQueries(session).avvis(
            entityId = id,
            opprettetAv = navIdent,
            aarsaker = emptyList(),
            forklaring = null,
        )

        @Language("PostgreSQL")
        val query = """
            update tilsagn set status = 'GODKJENT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        session.execute(queryOf(query, mapOf("id" to id)))
    }

    private fun Row.toTilsagnDto(): TilsagnDto {
        val status = TilsagnStatus.valueOf(string("status"))
        val sistHandling = Json.decodeFromString<ToTrinnskontrollHandlingDto<TilsagnStatusAarsak>>(string("sist_handling_json"))

        return TilsagnDto(
            id = uuid("id"),
            gjennomforing = TilsagnDto.Gjennomforing(
                id = uuid("gjennomforing_id"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            ),
            periodeSlutt = localDate("periode_slutt"),
            periodeStart = localDate("periode_start"),
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
            beregning = Json.decodeFromString<TilsagnBeregning>(string("beregning")),
            status = status,
            type = TilsagnType.valueOf(string("type")),
            sistHandling = sistHandling,
        )
    }

    private fun Row.toArrangorflateTilsagn(): ArrangorflateTilsagn {
        val aarsaker = arrayOrNull<String>("status_aarsaker")
            ?.toList()
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
