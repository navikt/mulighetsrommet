package no.nav.mulighetsrommet.oppgaver

import kotlinx.serialization.json.Json
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.db.createArrayOfAvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.tilsagn.db.createArrayOfTilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tiltakstype.db.createArrayOfTiltakskode
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.database.datatypes.toDaterange
import no.nav.mulighetsrommet.model.*
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class OppgaveQueries(private val session: Session) {
    fun getGjennomforingOppgaveData(
        tiltakskoder: Set<Tiltakskode>,
    ): List<GjennomforingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                gjennomforing.id,
                gjennomforing.navn,
                gjennomforing.updated_at,
                tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                tiltakstype.navn as tiltakstype_navn,
                (
                    select jsonb_agg(
                        jsonb_build_object(
                            'enhetsnummer', nav_enhet.enhetsnummer,
                            'navn', nav_enhet.navn,
                            'type', nav_enhet.type,
                            'overordnetEnhet', nav_enhet.overordnet_enhet
                        )
                    )
                    from gjennomforing_nav_enhet
                    join nav_enhet on nav_enhet.enhetsnummer = gjennomforing_nav_enhet.enhetsnummer
                    where gjennomforing_nav_enhet.gjennomforing_id = gjennomforing.id
                ) as nav_enheter_json,
                case when nav_enhet_arena.enhetsnummer is null
                    then null
                    else jsonb_build_object(
                        'enhetsnummer', nav_enhet_arena.enhetsnummer,
                        'navn', nav_enhet_arena.navn,
                        'type', nav_enhet_arena.type,
                        'overordnetEnhet', nav_enhet_arena.overordnet_enhet
                    )
                end as arena_ansvarlig_enhet_json
            from gjennomforing
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
                left join gjennomforing_administrator on gjennomforing_administrator.gjennomforing_id = gjennomforing.id
                left join nav_enhet nav_enhet_arena on nav_enhet_arena.enhetsnummer = gjennomforing.arena_ansvarlig_enhet
            where
                (:tiltakskoder::tiltakskode[] is null or tiltakstype.tiltakskode = any(:tiltakskoder::tiltakskode[]))
                and gjennomforing.status = 'GJENNOMFORES'
            group by gjennomforing.id, tiltakstype.tiltakskode, tiltakstype.navn, nav_enhet_arena.enhetsnummer
            having count(gjennomforing_administrator.nav_ident) = 0
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { session.createArrayOfTiltakskode(it) },
        )

        return session.list(queryOf(query, params)) {
            val arenaAnsvarligEnhet = it.stringOrNull("arena_ansvarlig_enhet_json")
                ?.let { json -> Json.decodeFromString<NavEnhetDto?>(json) }
            val navEnheter = it.stringOrNull("nav_enheter_json")
                ?.let { json -> Json.decodeFromString<List<NavEnhetDto>>(json) }
                ?.plus(arenaAnsvarligEnhet)
                ?.filterNotNull()
                ?: emptyList()
            val kontorstruktur = fromNavEnheter(navEnheter)

            GjennomforingOppgaveData(
                id = it.uuid("id"),
                navn = it.string("navn"),
                updatedAt = it.localDateTime("updated_at"),
                tiltakskode = Tiltakskode.valueOf(it.string("tiltakstype_tiltakskode")),
                tiltakstypeNavn = it.string("tiltakstype_navn"),
                kontorstruktur = kontorstruktur,
            )
        }
    }

    fun getDelutbetalingOppgaveData(
        kostnadssteder: Set<NavEnhetNummer>?,
        tiltakskoder: Set<Tiltakskode>?,
    ): List<DelutbetalingOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
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
                tilsagn.gjennomforing_id,
                nav_enhet.navn as kostnadssted_navn,
                nav_enhet.enhetsnummer as kostnadssted_enhetsnummer,
                gjennomforing.navn,
                tiltakstype.tiltakskode,
                tiltakstype.navn as tiltakstype_navn,
                totrinnskontroll.besluttet_tidspunkt,
                totrinnskontroll.behandlet_tidspunkt,
                totrinnskontroll.behandlet_av
            from delutbetaling
                inner join tilsagn on tilsagn.id = delutbetaling.tilsagn_id
                inner join nav_enhet on tilsagn.kostnadssted = nav_enhet.enhetsnummer
                inner join gjennomforing on gjennomforing.id = tilsagn.gjennomforing_id
                inner join tiltakstype on tiltakstype.id = gjennomforing.tiltakstype_id
                inner join totrinnskontroll on totrinnskontroll.entity_id = delutbetaling.id
            where
                totrinnskontroll.type = 'OPPRETT'
                and (:tiltakskoder::tiltakskode[] is null or tiltakstype.tiltakskode = any(:tiltakskoder::tiltakskode[]))
                and (:kostnadssteder::text[] is null or tilsagn.kostnadssted = any(:kostnadssteder))
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
            "kostnadssteder" to kostnadssteder?.let { session.createArrayOfValue(it) { it.value } },
        )

        return session.list(queryOf(query, params)) {
            DelutbetalingOppgaveData(
                tilsagnId = it.uuid("tilsagn_id"),
                utbetalingId = it.uuid("utbetaling_id"),
                id = it.uuid("id"),
                periode = it.periode("periode"),
                status = DelutbetalingStatus.valueOf(it.string("status")),
                gjennomforingId = it.uuid("gjennomforing_id"),
                gjennomforingNavn = it.string("navn"),
                tiltakstype = OppgaveTiltakstype(
                    tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                    navn = it.string("tiltakstype_navn"),
                ),
                kostnadssted = DelutbetalingOppgaveData.Kostnadssted(
                    navn = it.string("kostnadssted_navn"),
                    enhetsnummer = NavEnhetNummer(it.string("kostnadssted_enhetsnummer")),
                ),
                opprettelse = DelutbetalingOppgaveData.Opprettelse(
                    behandletAv = it.string("behandlet_av").toAgent(),
                    behandletTidspunkt = it.localDateTime("behandlet_tidspunkt"),
                    besluttetTidspunkt = it.localDateTimeOrNull("besluttet_tidspunkt"),
                ),
            )
        }
    }

    fun getTilsagnOppgaveData(): List<TilsagnOppgaveData> {
        @Language("PostgreSQL")
        val query = """
            select
                tilsagn.id,
                tilsagn.gjennomforing_id,
                tilsagn.bestillingsnummer,
                tilsagn.bestilling_status,
                tilsagn.belop_brukt,
                tilsagn.kostnadssted,
                tilsagn.status,
                nav_enhet.navn                    as kostnadssted_navn,
                gjennomforing.navn                as gjennomforing_navn,
                tiltakstype.tiltakskode           as tiltakskode,
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

            )
                .let { session.createArrayOfTilsagnStatus(it) },
        )

        return session.list(queryOf(query, params)) {
            TilsagnOppgaveData(
                id = it.uuid("id"),
                status = TilsagnStatus.valueOf(it.string("status")),
                gjennomforingId = it.uuid("gjennomforing_id"),
                belopBrukt = it.int("belop_brukt"),
                gjennomforingNavn = it.string("gjennomforing_navn"),
                kostnadssted = NavEnhetNummer(it.string("kostnadssted")),
                kostnadsstedNavn = it.string("kostnadssted_navn"),
                bestillingsnummer = it.string("bestillingsnummer"),
                tiltakstype = OppgaveTiltakstype(
                    tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                    navn = it.string("tiltakstype_navn"),
                ),
            )
        }
    }

    fun getUtbetalingOppgaveData(tiltakskoder: Set<Tiltakskode>?): List<UtbetalingOppgaveData> {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select * from utbetaling_dto_view
            where (:tiltakskoder::tiltakskode[] is null or tiltakskode = any(:tiltakskoder::tiltakskode[]))
        """.trimIndent()

        val params = mapOf(
            "tiltakskoder" to tiltakskoder?.let { session.createArrayOfTiltakskode(it) },
        )

        return session.list(queryOf(utbetalingQuery, params)) {
            UtbetalingOppgaveData(
                id = it.uuid("id"),
                gjennomforingId = it.uuid("gjennomforing_id"),
                gjennomforingNavn = it.string("gjennomforing_navn"),
                tiltakstype = OppgaveTiltakstype(
                    navn = it.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                ),
                periode = it.periode("periode"),
                createdAt = it.localDateTime("created_at"),
                status = UtbetalingStatusType.valueOf(it.string("status")),
            )
        }
    }

    fun getUtbetalingKostnadssteder(gjennomforingId: UUID, periodeIntersectsWith: Periode): List<NavEnhetNummer> {
        @Language("PostgreSQL")
        val utbetalingQuery = """
            select kostnadssted from tilsagn
            where
                (gjennomforing_id = :gjennomforing_id::uuid)
                and (periode && :periode::daterange)
        """.trimIndent()

        val params = mapOf(
            "gjennomforing_id" to gjennomforingId,
            "periode" to periodeIntersectsWith.toDaterange(),
        )

        return session.list(queryOf(utbetalingQuery, params)) {
            NavEnhetNummer(it.string("kostnadssted"))
        }
    }

    fun getAvtaleOppgaveData(
        tiltakskoder: Set<Tiltakskode>,
        navRegioner: List<NavEnhetNummer> = emptyList(),
    ): List<AvtaleOppgaveData> = with(session) {
        val statuser = listOf(AvtaleStatusType.UTKAST, AvtaleStatusType.AKTIV)

        val parameters = mapOf(
            "nav_enheter" to navRegioner.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { session.createArrayOfTiltakskode(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOfAvtaleStatus(statuser) },
        )

        @Language("PostgreSQL")
        val query = """
            select
                avtale.id,
                avtale.created_at,
                avtale.navn,
                tiltakstype.navn                                 as tiltakstype_navn,
                tiltakstype.tiltakskode                          as tiltakskode,
                nav_enheter_json
            from avtale
                join tiltakstype on tiltakstype.id = avtale.tiltakstype_id
                left join arrangor on arrangor.id = avtale.arrangor_hovedenhet_id
                left join nav_enhet arena_nav_enhet on avtale.arena_ansvarlig_enhet = arena_nav_enhet.enhetsnummer
                left join avtale_administrator on avtale_administrator.avtale_id = avtale.id
                left join lateral (select jsonb_agg(
                    jsonb_build_object(
                        'enhetsnummer', avtale_nav_enhet.enhetsnummer,
                        'navn', nav_enhet.navn,
                        'type', nav_enhet.type,
                        'overordnetEnhet', nav_enhet.overordnet_enhet
                    )
                ) as nav_enheter_json from avtale_nav_enhet
                    left join nav_enhet on nav_enhet.enhetsnummer = avtale_nav_enhet.enhetsnummer
                    where avtale_nav_enhet.avtale_id = avtale.id) on true
            where
                (:tiltakskoder::tiltakskode[] is null or tiltakstype.tiltakskode = any(:tiltakskoder::tiltakskode[]))
                and (:nav_enheter::text[] is null or (
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   avtale.arena_ansvarlig_enhet = any (:nav_enheter) or
                   avtale.arena_ansvarlig_enhet in (select enhetsnummer
                                                    from nav_enhet
                                                    where overordnet_enhet = any (:nav_enheter))))
                and (:statuser::text[] is null or avtale.status = any(:statuser))
            group by avtale.id, tiltakstype.tiltakskode, tiltakstype.navn, nav_enheter_json
            having count(avtale_administrator.nav_ident) = 0
        """.trimIndent()

        return session.list(queryOf(query, parameters)) {
            val navEnheter = it.stringOrNull("nav_enheter_json")
                ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
                ?: emptyList()

            AvtaleOppgaveData(
                id = it.uuid("id"),
                navn = it.string("navn"),
                kontorstruktur = fromNavEnheter(navEnheter),
                tiltakstype = OppgaveTiltakstype(
                    navn = it.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(it.string("tiltakskode")),
                ),
                createdAt = it.localDateTime("created_at"),
            )
        }
    }
}

data class GjennomforingOppgaveData(
    val id: UUID,
    val navn: String,
    val kontorstruktur: List<Kontorstruktur>,
    val tiltakskode: Tiltakskode,
    val tiltakstypeNavn: String,
    val updatedAt: LocalDateTime,
)

data class DelutbetalingOppgaveData(
    val id: UUID,
    val status: DelutbetalingStatus,
    val periode: Periode,
    val gjennomforingId: UUID,
    val utbetalingId: UUID,
    val tilsagnId: UUID,
    val gjennomforingNavn: String,
    val tiltakstype: OppgaveTiltakstype,
    val kostnadssted: Kostnadssted,
    val opprettelse: Opprettelse,
) {
    data class Kostnadssted(
        val navn: String,
        val enhetsnummer: NavEnhetNummer,
    )
    data class Opprettelse(
        val behandletTidspunkt: LocalDateTime,
        val besluttetTidspunkt: LocalDateTime?,
        val behandletAv: Agent,
    )
}

data class TilsagnOppgaveData(
    val id: UUID,
    val belopBrukt: Int,
    val gjennomforingId: UUID,
    val gjennomforingNavn: String,
    val status: TilsagnStatus,
    val tiltakstype: OppgaveTiltakstype,
    val kostnadssted: NavEnhetNummer,
    val kostnadsstedNavn: String,
    val bestillingsnummer: String,
)

data class UtbetalingOppgaveData(
    val id: UUID,
    val status: UtbetalingStatusType,
    val gjennomforingId: UUID,
    val gjennomforingNavn: String,
    val periode: Periode,
    val tiltakstype: OppgaveTiltakstype,
    val createdAt: LocalDateTime,
)

data class AvtaleOppgaveData(
    val id: UUID,
    val navn: String,
    val createdAt: LocalDateTime,
    val kontorstruktur: List<Kontorstruktur>,
    val tiltakstype: OppgaveTiltakstype,
)
