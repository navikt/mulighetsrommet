package no.nav.mulighetsrommet.api.tilsagn.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.Input
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri.Output
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
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
                belop_brukt,
                belop_beregnet,
                beregning_type,
                beregning_sats,
                beregning_antall_plasser,
                beregning_antall_timer_oppfolging_per_deltaker,
                beregning_prisbetingelser,
                kommentar,
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
                :belop_brukt,
                :belop_beregnet,
                :beregning_type::tilsagn_beregning_type,
                :beregning_sats,
                :beregning_antall_plasser,
                :beregning_antall_timer_oppfolging_per_deltaker,
                :beregning_prisbetingelser,
                :datastream_periode_start,
                :datastream_periode_slutt,
                :kommentar
            )
            on conflict (id) do update set
                gjennomforing_id                        = excluded.gjennomforing_id,
                periode                                 = excluded.periode,
                lopenummer                              = excluded.lopenummer,
                bestillingsnummer                       = excluded.bestillingsnummer,
                bestilling_status                       = excluded.bestilling_status,
                kostnadssted                            = excluded.kostnadssted,
                status                                  = excluded.status,
                type                                    = excluded.type,
                belop_brukt                             = excluded.belop_brukt,
                belop_beregnet                          = excluded.belop_beregnet,
                beregning_type                          = excluded.beregning_type,
                beregning_sats                                 = excluded.beregning_sats,
                beregning_antall_plasser                       = excluded.beregning_antall_plasser,
                beregning_antall_timer_oppfolging_per_deltaker = excluded.beregning_antall_timer_oppfolging_per_deltaker,
                beregning_prisbetingelser                      = excluded.beregning_prisbetingelser,
                kommentar                               = excluded.kommentar,
                datastream_periode_start                = excluded.datastream_periode_start,
                datastream_periode_slutt                = excluded.datastream_periode_slutt
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
            "belop_brukt" to dbo.belopBrukt,
            "belop_beregnet" to dbo.beregning.output.belop,
            "beregning_type" to when (dbo.beregning) {
                is TilsagnBeregningFri -> TilsagnBeregningType.FRI
                is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED
                is TilsagnBeregningPrisPerManedsverk -> TilsagnBeregningType.PRIS_PER_MANEDSVERK
                is TilsagnBeregningPrisPerUkesverk -> TilsagnBeregningType.PRIS_PER_UKESVERK
                is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING
            }.name,
            "datastream_periode_start" to dbo.periode.start,
            "datastream_periode_slutt" to dbo.periode.getLastInclusiveDate(),
            "kommentar" to dbo.kommentar,
        )
        val beregningParams = when (dbo.beregning) {
            is TilsagnBeregningFri -> mapOf(
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
            )

            is TilsagnBeregningPrisPerManedsverk -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerUkesverk -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> mapOf(
                "beregning_sats" to dbo.beregning.input.sats,
                "beregning_antall_plasser" to dbo.beregning.input.antallPlasser,
                "beregning_antall_timer_oppfolging_per_deltaker" to dbo.beregning.input.antallTimerOppfolgingPerDeltaker,
                "beregning_prisbetingelser" to dbo.beregning.input.prisbetingelser,
            )
        }

        execute(queryOf(query, params + beregningParams))

        if (dbo.beregning is TilsagnBeregningFri) {
            upsertTilsagnBeregningFriLinjer(dbo.id, dbo.beregning.input.linjer)
        }
    }

    private fun TransactionalSession.upsertTilsagnBeregningFriLinjer(
        tilsagnId: UUID,
        linjer: List<TilsagnBeregningFri.InputLinje>,
    ) {
        @Language("PostgreSQL")
        val deleteExistingQuery = """
            delete from tilsagn_fri_beregning
            where tilsagn_id = ?
        """.trimIndent()
        execute(queryOf(deleteExistingQuery, tilsagnId))

        @Language("PostgreSQL")
        val query = """
            insert into tilsagn_fri_beregning (
                    id,
                    tilsagn_id,
                    beskrivelse,
                    belop,
                    antall
                ) values (
                    :id::uuid,
                    :tilsagn_id::uuid,
                    :beskrivelse,
                    :belop,
                    :antall
                )
        """.trimIndent()
        val paramCollection = linjer.map {
            mapOf(
                "id" to it.id,
                "tilsagn_id" to tilsagnId,
                "beskrivelse" to it.beskrivelse,
                "belop" to it.belop,
                "antall" to it.antall,
            )
        }

        batchPreparedNamedStatement(query, paramCollection)
    }

    fun setBruktBelop(id: UUID, belop: Int) {
        @Language("PostgreSQL")
        val query = """
            update tilsagn set
                belop_brukt = :belop
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

    fun getOrError(id: UUID): Tilsagn {
        return checkNotNull(get(id)) { "Tilsagn med id $id finnes ikke" }
    }

    fun get(id: UUID): Tilsagn? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toTilsagnDto() }
    }

    fun getOrError(bestillingsnummer: String): Tilsagn {
        return checkNotNull(get(bestillingsnummer)) { "Tilsagn med bestillingsnummer $bestillingsnummer finnes ikke" }
    }

    fun get(bestillingsnummer: String): Tilsagn? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_admin_dto_view
            where bestillingsnummer = ?
        """.trimIndent()

        return session.single(queryOf(query, bestillingsnummer)) { it.toTilsagnDto() }
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
            order by created_at desc
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
            update tilsagn
            set bestilling_status = ?
             where bestillingsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, status.name, bestillingsnummer))
    }

    private fun Row.toTilsagnDto(): Tilsagn {
        val id = uuid("id")

        val beregning = getBeregning(id, TilsagnBeregningType.valueOf(string("beregning_type")))

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
            belopBrukt = int("belop_brukt"),
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
            kommentar = stringOrNull("kommentar"),
        )
    }

    private fun Row.getBeregning(id: UUID, beregning: TilsagnBeregningType): TilsagnBeregning {
        return when (beregning) {
            TilsagnBeregningType.FRI -> {
                TilsagnBeregningFri(
                    input = Input(
                        linjer = getTilsagnBeregningFriLinjer(id),
                        prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                    ),
                    output = Output(
                        belop = int("belop_beregnet"),
                    ),
                )
            }

            TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED -> TilsagnBeregningFastSatsPerTiltaksplassPerManed(
                input = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats"),
                    antallPlasser = int("beregning_antall_plasser"),
                ),
                output = TilsagnBeregningFastSatsPerTiltaksplassPerManed.Output(
                    belop = int("belop_beregnet"),
                ),
            )

            TilsagnBeregningType.PRIS_PER_MANEDSVERK -> TilsagnBeregningPrisPerManedsverk(
                input = TilsagnBeregningPrisPerManedsverk.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats"),
                    antallPlasser = int("beregning_antall_plasser"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerManedsverk.Output(
                    belop = int("belop_beregnet"),
                ),
            )

            TilsagnBeregningType.PRIS_PER_UKESVERK -> TilsagnBeregningPrisPerUkesverk(
                input = TilsagnBeregningPrisPerUkesverk.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats"),
                    antallPlasser = int("beregning_antall_plasser"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerUkesverk.Output(
                    belop = int("belop_beregnet"),
                ),
            )

            TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING -> TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker(
                input = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Input(
                    periode = periode("periode"),
                    sats = int("beregning_sats"),
                    antallPlasser = int("beregning_antall_plasser"),
                    antallTimerOppfolgingPerDeltaker = int("beregning_antall_timer_oppfolging_per_deltaker"),
                    prisbetingelser = stringOrNull("beregning_prisbetingelser"),
                ),
                output = TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker.Output(
                    belop = int("belop_beregnet"),
                ),
            )
        }
    }

    private fun getTilsagnBeregningFriLinjer(tilsagnId: UUID): List<TilsagnBeregningFri.InputLinje> {
        @Language("PostgreSQL")
        val query = """
            select *
            from tilsagn_fri_beregning
            where tilsagn_id = ?::uuid
        """.trimIndent()
        return session.list(queryOf(query, tilsagnId)) {
            TilsagnBeregningFri.InputLinje(
                id = it.uuid("id"),
                beskrivelse = it.string("beskrivelse"),
                belop = it.int("belop"),
                antall = it.int("antall"),
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
