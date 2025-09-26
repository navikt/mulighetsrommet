package no.nav.mulighetsrommet.api.gjennomforing.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.gjennomforing.model.AvbrytGjennomforingAarsak
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.intellij.lang.annotations.Language
import java.util.*

class EnkeltplassQueries(private val session: Session) {
    fun upsert(enkeltplass: EnkeltplassDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into gjennomforing (
                id,
                navn,
                tiltakstype_id,
                arrangor_id,
                start_dato,
                slutt_dato,
                status
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :arrangor_id,
                :start_dato,
                :slutt_dato,
                :status::gjennomforing_status
            )
            on conflict (id) do update set
                navn                               = excluded.navn,
                tiltakstype_id                     = excluded.tiltakstype_id,
                arrangor_id                        = excluded.arrangor_id,
                start_dato                         = excluded.start_dato,
                slutt_dato                         = excluded.slutt_dato,
                status                             = excluded.status
        """.trimIndent()

        val params = mapOf(
            "id" to enkeltplass.id,
            "navn" to "",
            "tiltakstype_id" to enkeltplass.tiltakstypeId,
            "arrangor_id" to enkeltplass.arrangorId,
            "start_dato" to enkeltplass.startDato,
            "slutt_dato" to enkeltplass.sluttDato,
            "status" to enkeltplass.status.name,
        )
        execute(queryOf(query, params))

        @Language("PostgreSQL")
        val deleteUtdanningslop = """
            delete from gjennomforing_utdanningsprogram
            where gjennomforing_id = ?::uuid
        """.trimIndent()

        @Language("PostgreSQL")
        val insertUtdanningslop = """
            insert into gjennomforing_utdanningsprogram(
                gjennomforing_id,
                utdanning_id,
                utdanningsprogram_id
            )
            values(:gjennomforing_id::uuid, :utdanning_id::uuid, :utdanningsprogram_id::uuid)
        """.trimIndent()

        execute(queryOf(deleteUtdanningslop, enkeltplass.id))

        enkeltplass.utdanningslop?.also { utdanningslop ->
            val utdanninger = utdanningslop.utdanninger.map {
                mapOf(
                    "gjennomforing_id" to enkeltplass.id,
                    "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                    "utdanning_id" to it,
                )
            }
            batchPreparedNamedStatement(insertUtdanningslop, utdanninger)
        }

        AmoKategoriseringQueries.upsert(
            AmoKategoriseringQueries.Relation.GJENNOMFORING,
            enkeltplass.id,
            enkeltplass.amoKategorisering,
        )
    }

    fun getOrError(id: UUID): Enkeltplass {
        return checkNotNull(get(id)) { "Enkeltplass med id $id finnes ikke" }
    }

    fun get(id: UUID): Enkeltplass? {
        @Language("PostgreSQL")
        val query = """
            select *
            from gjennomforing_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toGjennomforingDto() }
    }

    fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete from gjennomforing
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }

    private fun Row.toGjennomforingDto(): Enkeltplass {
        val status = when (GjennomforingStatusType.valueOf(string("status"))) {
            GjennomforingStatusType.GJENNOMFORES -> GjennomforingStatus.Gjennomfores
            GjennomforingStatusType.AVSLUTTET -> GjennomforingStatus.Avsluttet

            GjennomforingStatusType.AVBRUTT -> GjennomforingStatus.Avbrutt(
                tidspunkt = localDateTime("avsluttet_tidspunkt"),
                array<String>("avbrutt_aarsaker").map { AvbrytGjennomforingAarsak.valueOf(it) },
                stringOrNull("avbrutt_forklaring"),
            )

            GjennomforingStatusType.AVLYST -> GjennomforingStatus.Avlyst(
                tidspunkt = localDateTime("avsluttet_tidspunkt"),
                array<String>("avbrutt_aarsaker").map { AvbrytGjennomforingAarsak.valueOf(it) },
                stringOrNull("avbrutt_forklaring"),
            )
        }

        return Enkeltplass(
            id = uuid("id"),
            lopenummer = string("lopenummer"),
            startDato = localDate("start_dato"),
            sluttDato = localDateOrNull("slutt_dato"),
            status = status,
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            opprettetTidspunkt = localDateTime("opprettet_tidspunkt"),
            oppdatertTidspunkt = localDateTime("oppdatert_tidspunkt"),
            arenaAnsvarligEnhet = stringOrNull("arena_nav_enhet_enhetsnummer")?.let {
                ArenaNavEnhet(
                    navn = stringOrNull("arena_nav_enhet_navn"),
                    enhetsnummer = it,
                )
            },
            arrangor = Enkeltplass.Arrangor(
                id = uuid("arrangor_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
                navn = string("arrangor_navn"),
                slettet = boolean("arrangor_slettet"),
            ),
            tiltakstype = Enkeltplass.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                tiltakskode = stringOrNull("tiltakstype_tiltakskode")?.let { Tiltakskode.valueOf(it) },
            ),
            amoKategorisering = stringOrNull("amo_kategorisering_json")?.let {
                JsonIgnoreUnknownKeys.decodeFromString(it)
            },
            utdanningslop = stringOrNull("utdanningslop_json")?.let {
                JsonIgnoreUnknownKeys.decodeFromString(it)
            },
        )
    }
}
