package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.db.createArrayOfAvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.tilsagn.db.createArrayOfTilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.toAgent
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class OppgaveQueries(private val session: Session) {
    fun getGjennomforingManglerAdministratorOppgaveData(
        tiltakskoder: Set<Tiltakskode>,
        navEnheter: Set<NavEnhetNummer>,
    ): List<GjennomforingManglerAdministratorOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                navn,
                oppdatert_tidspunkt,
                tiltakstype_tiltakskode,
                tiltakstype_navn,
                nav_enheter_json
            from view_gjennomforing
            where (:tiltakskoder::tiltakskode[] is null or tiltakstype_tiltakskode = any(:tiltakskoder::tiltakskode[]))
                and gjennomforing_type = 'AVTALE'
                and status = 'GJENNOMFORES'
                and (:nav_enheter::text[] is null or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)))
                and jsonb_array_length(coalesce(administratorer_json, '[]')) = 0
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { session.createArrayOfTiltakskode(it) },
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { session.createArrayOfValue(it) { it.value } },
        )

        return session.list(queryOf(query, params)) { row ->
            val navEnheter = row.stringOrNull("nav_enheter_json")
                ?.let { json -> Json.decodeFromString<List<NavEnhetDto>>(json) }
                ?: emptyList()
            val kontorstruktur = fromNavEnheter(navEnheter)

            GjennomforingManglerAdministratorOppgaveData(
                id = row.uuid("id"),
                navn = row.string("navn"),
                oppdatertTidspunkt = row.localDateTime("oppdatert_tidspunkt"),
                kontorstruktur = kontorstruktur,
                tiltakstype = row.toOppgaveTiltakstype(),
            )
        }
    }

    fun getDelutbetalingOppgaveData(
        kostnadssteder: Set<NavEnhetNummer>?,
        tiltakskoder: Set<Tiltakskode>?,
    ): List<DelutbetalingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            SELECT
                delutbetaling.id,
                delutbetaling.tilsagn_id,
                delutbetaling.utbetaling_id,
                delutbetaling.status,
                delutbetaling.belop,
                delutbetaling.gjor_opp_tilsagn,
                delutbetaling.periode,
                delutbetaling.lopenummer,
                delutbetaling.fakturanummer,
                delutbetaling.faktura_status,
                delutbetaling.faktura_status_sist_oppdatert,
                nav_enhet.navn AS kostnadssted_navn,
                nav_enhet.enhetsnummer AS kostnadssted_enhetsnummer,
                gjennomforing.id as gjennomforing_id,
                gjennomforing.lopenummer as gjennomforing_lopenummer,
                gjennomforing.navn as gjennomforing_navn,
                gjennomforing.gjennomforing_type,
                tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                tiltakstype.navn AS tiltakstype_navn,
                tk.besluttet_tidspunkt,
                tk.behandlet_tidspunkt,
                tk.behandlet_av
            FROM delutbetaling
            INNER JOIN tilsagn ON tilsagn.id = delutbetaling.tilsagn_id
            INNER JOIN nav_enhet ON tilsagn.kostnadssted = nav_enhet.enhetsnummer
            INNER JOIN gjennomforing ON gjennomforing.id = tilsagn.gjennomforing_id
            INNER JOIN tiltakstype ON tiltakstype.id = gjennomforing.tiltakstype_id
            INNER JOIN (
                SELECT DISTINCT ON (entity_id) *
                FROM totrinnskontroll
                WHERE type = 'OPPRETT'
                ORDER BY entity_id, behandlet_tidspunkt DESC
            ) tk ON tk.entity_id = delutbetaling.id
            WHERE
                (:tiltakskoder::tiltakskode[] IS NULL OR tiltakstype.tiltakskode = ANY(:tiltakskoder::tiltakskode[]))
                AND (:kostnadssteder::text[] IS NULL OR tilsagn.kostnadssted = ANY(:kostnadssteder))
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
            "kostnadssteder" to kostnadssteder?.let { session.createArrayOfValue(it) { it.value } },
        )

        return session.list(queryOf(query, params)) { row ->
            DelutbetalingOppgaveData(
                tilsagnId = row.uuid("tilsagn_id"),
                utbetalingId = row.uuid("utbetaling_id"),
                id = row.uuid("id"),
                periode = row.periode("periode"),
                status = DelutbetalingStatus.valueOf(row.string("status")),
                kostnadssted = OppgaveEnhet(
                    navn = row.string("kostnadssted_navn"),
                    nummer = NavEnhetNummer(row.string("kostnadssted_enhetsnummer")),
                ),
                opprettelse = DelutbetalingOppgaveData.Opprettelse(
                    behandletAv = row.string("behandlet_av").toAgent(),
                    behandletTidspunkt = row.localDateTime("behandlet_tidspunkt"),
                    besluttetTidspunkt = row.localDateTimeOrNull("besluttet_tidspunkt"),
                ),
                tiltakstype = row.toOppgaveTiltakstype(),
                gjennomforing = row.toOppgaveGjennomforing(),
            )
        }
    }

    fun getTilsagnOppgaveData(): List<TilsagnOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                tilsagn.id,
                tilsagn.bestillingsnummer,
                tilsagn.bestilling_status,
                tilsagn.belop_brukt,
                tilsagn.kostnadssted,
                tilsagn.status,
                nav_enhet.navn                    as kostnadssted_navn,
                gjennomforing.id                  as gjennomforing_id,
                gjennomforing.lopenummer          as gjennomforing_lopenummer,
                gjennomforing.navn                as gjennomforing_navn,
                gjennomforing.gjennomforing_type,
                tiltakstype.tiltakskode           as tiltakstype_tiltakskode,
                tiltakstype.navn                  as tiltakstype_navn
            from tilsagn
                inner join nav_enhet on nav_enhet.enhetsnummer = tilsagn.kostnadssted
                inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
            where
                (:statuser::tilsagn_status[] is null or tilsagn.status::tilsagn_status = any(:statuser))
            order by tilsagn.created_at desc
        """.trimIndent()

        val params = mapOf(
            "statuser" to listOf(
                TilsagnStatus.TIL_GODKJENNING,
                TilsagnStatus.TIL_ANNULLERING,
                TilsagnStatus.TIL_OPPGJOR,
                TilsagnStatus.RETURNERT,
            ).let { session.createArrayOfTilsagnStatus(it) },
        )

        return session.list(queryOf(query, params)) { row ->
            TilsagnOppgaveData(
                id = row.uuid("id"),
                status = TilsagnStatus.valueOf(row.string("status")),
                belopBrukt = row.int("belop_brukt"),
                kostnadssted = OppgaveEnhet(
                    navn = row.string("kostnadssted_navn"),
                    nummer = NavEnhetNummer(row.string("kostnadssted")),
                ),
                bestillingsnummer = row.string("bestillingsnummer"),
                tiltakstype = row.toOppgaveTiltakstype(),
                gjennomforing = row.toOppgaveGjennomforing(),
            )
        }
    }

    fun getUtbetalingOppgaveData(tiltakskoder: Set<Tiltakskode>?): List<UtbetalingOppgaveData> {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select
                utbetaling.id,
                utbetaling.periode,
                utbetaling.created_at,
                utbetaling.godkjent_av_arrangor_tidspunkt,
                utbetaling.status,
                gjennomforing.id as gjennomforing_id,
                gjennomforing.lopenummer as gjennomforing_lopenummer,
                gjennomforing.navn as gjennomforing_navn,
                gjennomforing.gjennomforing_type,
                tiltakstype.navn as tiltakstype_navn,
                tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                ks.kostnadssteder
            from utbetaling
                join gjennomforing on gjennomforing.id = utbetaling.gjennomforing_id
                join tiltakstype on gjennomforing.tiltakstype_id = tiltakstype.id
                left join lateral (
                    select array_agg(tilsagn.kostnadssted) as kostnadssteder
                    from tilsagn
                    where tilsagn.gjennomforing_id = utbetaling.gjennomforing_id
                      and tilsagn.periode && utbetaling.periode
                ) ks on true
            where
                (:tiltakskoder::tiltakskode[] is null or tiltakstype.tiltakskode = any(:tiltakskoder::tiltakskode[]));
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
        )

        return session.list(queryOf(utbetalingQuery, params)) { row ->
            UtbetalingOppgaveData(
                id = row.uuid("id"),
                periode = row.periode("periode"),
                createdAt = row.localDateTime("created_at"),
                godkjentAvArrangorTidspunkt = row.localDateTimeOrNull("godkjent_av_arrangor_tidspunkt"),
                status = UtbetalingStatusType.valueOf(row.string("status")),
                kostnadssteder = row.arrayOrNull<String>("kostnadssteder")?.map { NavEnhetNummer(it) } ?: emptyList(),
                tiltakstype = row.toOppgaveTiltakstype(),
                gjennomforing = row.toOppgaveGjennomforing(),
            )
        }
    }

    fun getAvtaleManglerAdministratorOppgaveData(
        tiltakskoder: Set<Tiltakskode>,
        navRegioner: Set<NavEnhetNummer>,
    ): List<AvtaleManglerAdministratorOppgaveData> = with(session) {
        val statuser = listOf(AvtaleStatusType.UTKAST, AvtaleStatusType.AKTIV)

        val parameters = mapOf(
            "nav_enheter" to navRegioner.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { session.createArrayOfTiltakskode(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOfAvtaleStatus(statuser) },
        )

        @Language("PostgreSQL")
        val query = """
            select
                id,
                oppdatert_tidspunkt,
                navn,
                tiltakstype_navn,
                tiltakstype_tiltakskode,
                nav_enheter_json
            from view_avtale
            where
                (:tiltakskoder::tiltakskode[] is null or tiltakstype_tiltakskode = any(:tiltakskoder::tiltakskode[]))
                and (:statuser::text[] is null or status = any(:statuser))
                and (:nav_enheter::text[] is null or
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)))
                and jsonb_array_length(coalesce(administratorer_json, '[]')) = 0
        """.trimIndent()

        return session.list(queryOf(query, parameters)) {
            val navEnheter = it.stringOrNull("nav_enheter_json")
                ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
                ?: emptyList()

            AvtaleManglerAdministratorOppgaveData(
                id = it.uuid("id"),
                navn = it.string("navn"),
                kontorstruktur = fromNavEnheter(navEnheter),
                tiltakstype = it.toOppgaveTiltakstype(),
                oppdatertTidspunkt = it.localDateTime("oppdatert_tidspunkt"),
            )
        }
    }
}

private fun Row.toOppgaveTiltakstype(): OppgaveTiltakstype = OppgaveTiltakstype(
    navn = string("tiltakstype_navn"),
    tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
)

data class GjennomforingManglerAdministratorOppgaveData(
    val id: UUID,
    val navn: String,
    val kontorstruktur: List<Kontorstruktur>,
    val oppdatertTidspunkt: LocalDateTime,
    val tiltakstype: OppgaveTiltakstype,
)

data class DelutbetalingOppgaveData(
    val id: UUID,
    val status: DelutbetalingStatus,
    val periode: Periode,
    val utbetalingId: UUID,
    val tilsagnId: UUID,
    val kostnadssted: OppgaveEnhet,
    val opprettelse: Opprettelse,
    val tiltakstype: OppgaveTiltakstype,
    val gjennomforing: OppgaveGjennomforing,
) {
    data class Opprettelse(
        val behandletTidspunkt: LocalDateTime,
        val besluttetTidspunkt: LocalDateTime?,
        val behandletAv: Agent,
    )
}

data class TilsagnOppgaveData(
    val id: UUID,
    val belopBrukt: Int,
    val status: TilsagnStatus,
    val kostnadssted: OppgaveEnhet,
    val bestillingsnummer: String,
    val tiltakstype: OppgaveTiltakstype,
    val gjennomforing: OppgaveGjennomforing,
)

data class UtbetalingOppgaveData(
    val id: UUID,
    val status: UtbetalingStatusType,
    val periode: Periode,
    val createdAt: LocalDateTime,
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    val kostnadssteder: List<NavEnhetNummer>,
    val tiltakstype: OppgaveTiltakstype,
    val gjennomforing: OppgaveGjennomforing,
)

data class AvtaleManglerAdministratorOppgaveData(
    val id: UUID,
    val navn: String,
    val oppdatertTidspunkt: LocalDateTime,
    val kontorstruktur: List<Kontorstruktur>,
    val tiltakstype: OppgaveTiltakstype,
)

private fun Row.toOppgaveGjennomforing(): OppgaveGjennomforing {
    val id = uuid("gjennomforing_id")
    return when (GjennomforingType.valueOf(string("gjennomforing_type"))) {
        GjennomforingType.ARENA -> throw IllegalStateException("Oppgaver er ikke støttet for gjennomføring av typen ARENA id=$id")

        GjennomforingType.AVTALE -> OppgaveGjennomforing.Gruppetiltak(
            id = id,
            lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
            navn = string("gjennomforing_navn"),
        )

        GjennomforingType.ENKELTPLASS -> OppgaveGjennomforing.Enkeltplass(
            id = id,
            lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
        )
    }
}

sealed class OppgaveGjennomforing {
    abstract val id: UUID
    abstract val lopenummer: Tiltaksnummer

    data class Gruppetiltak(
        override val id: UUID,
        override val lopenummer: Tiltaksnummer,
        val navn: String,
    ) : OppgaveGjennomforing()

    data class Enkeltplass(
        override val id: UUID,
        override val lopenummer: Tiltaksnummer,
    ) : OppgaveGjennomforing()
}
