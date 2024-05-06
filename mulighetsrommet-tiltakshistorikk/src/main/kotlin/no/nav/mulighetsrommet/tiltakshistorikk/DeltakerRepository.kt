package no.nav.mulighetsrommet.tiltakshistorikk

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(deltaker: ArenaDeltakerDbo) = db.useSession { session ->
        logger.info("Lagrer deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into arena_deltaker (id,
                                        norsk_ident,
                                        arena_tiltakskode,
                                        status,
                                        start_dato,
                                        slutt_dato,
                                        beskrivelse,
                                        arrangor_organisasjonsnummer,
                                        registrert_i_arena_dato)
            values (:id::uuid,
                    :norsk_ident,
                    :arena_tiltakskode,
                    :status::arena_deltaker_status,
                    :start_dato,
                    :slutt_dato,
                    :beskrivelse,
                    :arrangor_organisasjonsnummer,
                    :registrert_i_arena_dato)
            on conflict (id) do update set norsk_ident                  = excluded.norsk_ident,
                                           arena_tiltakskode            = excluded.arena_tiltakskode,
                                           status                       = excluded.status,
                                           start_dato                   = excluded.start_dato,
                                           slutt_dato                   = excluded.slutt_dato,
                                           beskrivelse                  = excluded.beskrivelse,
                                           arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                                           registrert_i_arena_dato      = excluded.registrert_i_arena_dato

        """.trimIndent()

        queryOf(query, deltaker.toSqlParameters()).asExecute.runWithSession(session)
    }

    fun getDeltakelser(identer: List<NorskIdent>) = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select *
            from arena_deltaker
            where norsk_ident = any(:identer)
            order by start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to identer.map { it.value }.let { session.createArrayOf("text", it) },
        )

        queryOf(query, params).map { it.toArenaDeltaker() }.asList.runWithSession(session)
    }

    fun delete(id: UUID) = db.useSession { session ->
        logger.info("Sletter deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from arena_deltaker
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id).asExecute.runWithSession(session)
    }
}

private fun ArenaDeltakerDbo.toSqlParameters() = mapOf(
    "id" to id,
    "norsk_ident" to norskIdent.value,
    "arena_tiltakskode" to arenaTiltakskode,
    "status" to status.name,
    "start_dato" to startDato,
    "slutt_dato" to sluttDato,
    "registrert_i_arena_dato" to registrertIArenaDato,
    "beskrivelse" to beskrivelse,
    "arrangor_organisasjonsnummer" to arrangorOrganisasjonsnummer.value,
)

private fun Row.toArenaDeltaker(): ArenaDeltakerDbo = ArenaDeltakerDbo(
    id = uuid("id"),
    norskIdent = NorskIdent(string("norsk_ident")),
    arenaTiltakskode = string("arena_tiltakskode"),
    status = ArenaDeltakerStatus.valueOf(string("status")),
    startDato = localDateTimeOrNull("start_dato"),
    sluttDato = localDateTimeOrNull("slutt_dato"),
    registrertIArenaDato = localDateTime("registrert_i_arena_dato"),
    beskrivelse = string("beskrivelse"),
    arrangorOrganisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
)
