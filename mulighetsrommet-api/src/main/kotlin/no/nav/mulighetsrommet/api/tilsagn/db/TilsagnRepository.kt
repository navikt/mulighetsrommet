package no.nav.mulighetsrommet.api.tilsagn.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class TilsagnRepository(private val db: Database) {
    fun upsert(dbo: TilsagnDbo) = db.transaction {
        upsert(dbo, it)
    }

    fun upsert(dbo: TilsagnDbo, tx: Session) {
        @Language("PostgreSQL")
        val query = """
            insert into tilsagn (
                id,
                tiltaksgjennomforing_id,
                periode_start,
                periode_slutt,
                kostnadssted,
                arrangor_id,
                beregning,
                status_endret_av,
                status_endret_tidspunkt,
                status,
                type
            ) values (
                :id::uuid,
                :tiltaksgjennomforing_id::uuid,
                :periode_start,
                :periode_slutt,
                :kostnadssted,
                :arrangor_id::uuid,
                :beregning::jsonb,
                :status_endret_av,
                :status_endret_tidspunkt,
                'TIL_GODKJENNING'::tilsagn_status,
                :type::tilsagn_type
            )
            on conflict (id) do update set
                tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                periode_start           = excluded.periode_start,
                periode_slutt           = excluded.periode_slutt,
                kostnadssted            = excluded.kostnadssted,
                arrangor_id             = excluded.arrangor_id,
                beregning               = excluded.beregning,
                status_endret_av        = excluded.status_endret_av,
                status_endret_tidspunkt = excluded.status_endret_tidspunkt,
                status                  = excluded.status,
                type                    = excluded.type
        """.trimIndent()

        val params = mapOf(
            "id" to dbo.id,
            "tiltaksgjennomforing_id" to dbo.tiltaksgjennomforingId,
            "periode_start" to dbo.periodeStart,
            "periode_slutt" to dbo.periodeSlutt,
            "kostnadssted" to dbo.kostnadssted,
            "arrangor_id" to dbo.arrangorId,
            "beregning" to Json.encodeToString<TilsagnBeregning>(dbo.beregning),
            "status_endret_av" to dbo.endretAv.value,
            "status_endret_tidspunkt" to dbo.endretTidspunkt,
            "type" to dbo.type.name,
        )

        tx.run(queryOf(query, params).asExecute)
    }

    fun get(id: UUID) = db.transaction {
        get(id, it)
    }

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

    fun getAll(
        type: TilsagnType? = null,
        gjennomforingId: UUID? = null,
        statuser: List<TilsagnStatus>? = null,
    ): List<TilsagnDto> = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_admin_dto_view
            where (:type::tilsagn_type is null or type = :type::tilsagn_type)
              and (:gjennomforing_id::uuid is null or tiltaksgjennomforing_id = :gjennomforing_id::uuid)
              and (:statuser::tilsagn_status[] is null or status = any(:statuser))
            order by lopenummer desc
        """.trimIndent()

        val params = mapOf(
            "type" to type?.name,
            "gjennomforing_id" to gjennomforingId,
            "statuser" to statuser?.let { session.createArrayOf("tilsagn_status", statuser) },
        )

        queryOf(query, params)
            .map { it.toTilsagnDto() }
            .asList
            .runWithSession(session)
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
        periode: RefusjonskravPeriode,
    ): List<ArrangorflateTilsagn> {
        @Language("PostgreSQL")
        val query = """
            select * from tilsagn_arrangorflate_view
            where gjennomforing_id = :gjennomforing_id::uuid
              and (periode_start <= :periode_slutt::date)
              and (periode_slutt >= :periode_start::date)
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "gjennomforing_id" to gjennomforingId,
                "periode_start" to periode.start,
                "periode_slutt" to periode.slutt,
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

    fun delete(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from tilsagn where id = :id::uuid
        """.trimIndent()

        db.run(queryOf(query, mapOf("id" to id)).asExecute)
    }

    fun besluttGodkjennelse(
        id: UUID,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
        tx: TransactionalSession,
    ) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                status_besluttet_av = :nav_ident,
                status_endret_tidspunkt = :tidspunkt,
                status = 'GODKJENT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        tx.run(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "nav_ident" to navIdent.value,
                    "tidspunkt" to tidspunkt,
                ),
            ).asUpdate,
        )
    }

    fun returner(
        id: UUID,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
        tx: Session,
    ) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                status_besluttet_av = :nav_ident,
                status_endret_tidspunkt = :tidspunkt,
                status_aarsaker = :status_aarsaker::tilsagn_status_aarsak[],
                status_forklaring = :status_forklaring,
                status = 'RETURNERT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        tx.run(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "nav_ident" to navIdent.value,
                    "tidspunkt" to tidspunkt,
                    "status_aarsaker" to aarsaker.map { it.name }.let { db.createTextArray(it) },
                    "status_forklaring" to forklaring,
                ),
            ).asUpdate,
        )
    }

    fun tilAnnullering(
        id: UUID,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
        aarsaker: List<TilsagnStatusAarsak>,
        forklaring: String?,
        tx: Session,
    ) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                status_endret_av = :nav_ident,
                status_besluttet_av = null,
                status_endret_tidspunkt = :tidspunkt,
                status_aarsaker = :status_aarsaker::tilsagn_status_aarsak[],
                status_forklaring = :status_forklaring,
                status = 'TIL_ANNULLERING'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        tx.run(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "nav_ident" to navIdent.value,
                    "tidspunkt" to tidspunkt,
                    "status_aarsaker" to aarsaker.map { it.name }.let { db.createTextArray(it) },
                    "status_forklaring" to forklaring,
                ),
            ).asUpdate,
        )
    }

    fun besluttAnnullering(
        id: UUID,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
        tx: Session,
    ) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                status_besluttet_av = :nav_ident,
                status_endret_tidspunkt = :tidspunkt,
                status = 'ANNULLERT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        tx.run(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "nav_ident" to navIdent.value,
                    "tidspunkt" to tidspunkt,
                ),
            ).asUpdate,
        )
    }

    fun avbrytAnnullering(
        id: UUID,
        navIdent: NavIdent,
        tidspunkt: LocalDateTime,
        tx: Session,
    ) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                status_endret_av = :nav_ident,
                status_endret_tidspunkt = :tidspunkt,
                status = 'GODKJENT'::tilsagn_status
            where id = :id::uuid
        """.trimIndent()

        tx.run(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "nav_ident" to navIdent.value,
                    "tidspunkt" to tidspunkt,
                ),
            ).asUpdate,
        )
    }

    private fun Row.toTilsagnDto(): TilsagnDto {
        val aarsaker = arrayOrNull<String>("status_aarsaker")
            ?.toList()
            ?.map { TilsagnStatusAarsak.valueOf(it) } ?: emptyList()
        val forklaring = stringOrNull("status_forklaring")

        val status = toTilsagnStatus(
            status = TilsagnStatus.valueOf(string("status")),
            endretAv = NavIdent(string("status_endret_av")),
            endretTidspunkt = localDateTime("status_endret_tidspunkt"),
            besluttetAv = stringOrNull("status_besluttet_av")?.let { NavIdent(it) },
            aarsaker = aarsaker,
            forklaring = forklaring,
            besluttetAvNavn = stringOrNull("beslutter_navn"),
            endretAvNavn = string("endret_av_navn"),
        )

        return TilsagnDto(
            id = uuid("id"),
            tiltaksgjennomforing = TilsagnDto.Tiltaksgjennomforing(
                id = uuid("tiltaksgjennomforing_id"),
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
            beregning = Json.decodeFromString<TilsagnBeregning>(string("beregning")),
        )
    }
}

fun toTilsagnStatus(
    status: TilsagnStatus,
    endretAv: NavIdent,
    endretAvNavn: String,
    besluttetAv: NavIdent?,
    besluttetAvNavn: String?,
    endretTidspunkt: LocalDateTime,
    aarsaker: List<TilsagnStatusAarsak>,
    forklaring: String?,
): TilsagnDto.TilsagnStatus = when (status) {
    TilsagnStatus.TIL_GODKJENNING -> TilsagnDto.TilsagnStatus.TilGodkjenning(
        endretAv = endretAv,
        endretTidspunkt = endretTidspunkt,
    )

    TilsagnStatus.GODKJENT -> TilsagnDto.TilsagnStatus.Godkjent

    TilsagnStatus.RETURNERT -> {
        requireNotNull(besluttetAv)
        requireNotNull(besluttetAvNavn)
        TilsagnDto.TilsagnStatus.Returnert(
            endretAv = endretAv,
            endretTidspunkt = endretTidspunkt,
            returnertAv = besluttetAv,
            returnertAvNavn = besluttetAvNavn,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
    }

    TilsagnStatus.TIL_ANNULLERING -> TilsagnDto.TilsagnStatus.TilAnnullering(
        endretAv = endretAv,
        endretAvNavn = endretAvNavn,
        endretTidspunkt = endretTidspunkt,
        aarsaker = aarsaker,
        forklaring = forklaring,
    )

    TilsagnStatus.ANNULLERT -> {
        requireNotNull(besluttetAv)
        TilsagnDto.TilsagnStatus.Annullert(
            endretAv = endretAv,
            endretTidspunkt = endretTidspunkt,
            godkjentAv = besluttetAv,
            aarsaker = aarsaker,
            forklaring = forklaring,
        )
    }
}
