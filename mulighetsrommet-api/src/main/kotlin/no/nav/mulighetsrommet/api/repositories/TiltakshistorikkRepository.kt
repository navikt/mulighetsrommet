package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.TiltakshistorikkDbo
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class TiltakshistorikkRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakshistorikk: ArenaTiltakshistorikkDbo): ArenaTiltakshistorikkDbo {
        logger.info("Lagrer tiltakshistorikk id=${tiltakshistorikk.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakshistorikk (id, tiltaksgjennomforing_id, norsk_ident, status, fra_dato, til_dato, beskrivelse, arrangor_organisasjonsnummer, tiltakstypeid, registrert_i_arena_dato)
            values (:id::uuid, :tiltaksgjennomforing_id::uuid, :norsk_ident, :status::deltakerstatus, :fra_dato, :til_dato, :beskrivelse, :arrangor_organisasjonsnummer, :tiltakstypeid::uuid, :registrert_i_arena_dato)
            on conflict (id)
                do update set tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              norsk_ident             = excluded.norsk_ident,
                              status                  = excluded.status,
                              fra_dato                = excluded.fra_dato,
                              til_dato                = excluded.til_dato,
                              beskrivelse             = excluded.beskrivelse,
                              arrangor_organisasjonsnummer       = excluded.arrangor_organisasjonsnummer,
                              tiltakstypeid           = excluded.tiltakstypeid,
                              registrert_i_arena_dato = excluded.registrert_i_arena_dato
            returning *
        """.trimIndent()

        return queryOf(query, tiltakshistorikk.toSqlParameters())
            .map { it.toTiltakshistorikkDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter tiltakshistorikk id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakshistorikk
            where id = ?::uuid
        """.trimIndent()

        run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }

    fun deleteByExpirationDate(date: LocalDate): QueryResult<Int> = query {
        @Language("PostgreSQL")
        val query = """
            delete from tiltakshistorikk
            where til_dato < :date or til_dato is null and registrert_i_arena_dato < :date
        """.trimIndent()

        run { queryOf(query, mapOf("date" to date)) }
            .asUpdate
            .let { db.run(it) }
    }

    fun getTiltakshistorikkForBruker(identer: List<String>): List<TiltakshistorikkDbo> {
        @Language("PostgreSQL")
        val query = """
            select h.id,
                   h.fra_dato,
                   h.til_dato,
                   h.status,
                   coalesce(g.navn, h.beskrivelse) as navn,
                   coalesce(v.organisasjonsnummer, h.arrangor_organisasjonsnummer) as arrangor_organisasjonsnummer,
                   t.navn as tiltakstype
            from tiltakshistorikk h
                     left join tiltaksgjennomforing g on g.id = h.tiltaksgjennomforing_id
                     left join tiltakstype t on t.id = coalesce(g.tiltakstype_id, h.tiltakstypeid)
                     left join virksomhet v on v.id = g.arrangor_virksomhet_id
            where h.norsk_ident = any(?)
            order by h.fra_dato desc nulls last;
        """.trimIndent()
        val queryResult = queryOf(query, db.createTextArray(identer)).map { it.toTiltakshistorikk() }.asList
        return db.run(queryResult)
    }

    fun deleteTiltakshistorikkForIdenter(identer: List<String>) {
        @Language("PostgreSQL")
        val query = """
            delete from tiltakshistorikk where norsk_ident = any(?);
        """.trimIndent()

        db.run(queryOf(query, db.createTextArray(identer)).asExecute)
    }

    private fun ArenaTiltakshistorikkDbo.toSqlParameters(): Map<String, *> {
        return mapOf(
            "id" to id,
            "norsk_ident" to norskIdent,
            "status" to status.name,
            "fra_dato" to fraDato,
            "til_dato" to tilDato,
            "registrert_i_arena_dato" to registrertIArenaDato,
        ) + when (this) {
            is ArenaTiltakshistorikkDbo.Gruppetiltak -> listOfNotNull(
                "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
            )

            is ArenaTiltakshistorikkDbo.IndividueltTiltak -> listOfNotNull(
                "beskrivelse" to beskrivelse,
                "arrangor_organisasjonsnummer" to arrangorOrganisasjonsnummer,
                "tiltakstypeid" to tiltakstypeId,
            )
        }
    }

    private fun Row.toTiltakshistorikkDbo(): ArenaTiltakshistorikkDbo {
        return uuidOrNull("tiltaksgjennomforing_id")
            ?.let {
                ArenaTiltakshistorikkDbo.Gruppetiltak(
                    id = uuid("id"),
                    tiltaksgjennomforingId = it,
                    norskIdent = string("norsk_ident"),
                    status = Deltakerstatus.valueOf(string("status")),
                    fraDato = localDateTimeOrNull("fra_dato"),
                    tilDato = localDateTimeOrNull("til_dato"),
                    registrertIArenaDato = localDateTime("registrert_i_arena_dato"),
                )
            }
            ?: ArenaTiltakshistorikkDbo.IndividueltTiltak(
                id = uuid("id"),
                norskIdent = string("norsk_ident"),
                status = Deltakerstatus.valueOf(string("status")),
                fraDato = localDateTimeOrNull("fra_dato"),
                tilDato = localDateTimeOrNull("til_dato"),
                registrertIArenaDato = localDateTime("registrert_i_arena_dato"),
                beskrivelse = string("beskrivelse"),
                tiltakstypeId = uuid("tiltakstypeid"),
                arrangorOrganisasjonsnummer = string("arrangor_organisasjonsnummer"),
            )
    }

    private fun Row.toTiltakshistorikk() = TiltakshistorikkDbo(
        id = uuid("id"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(string("status")),
        tiltaksnavn = stringOrNull("navn"),
        tiltakstype = string("tiltakstype"),
        arrangorOrganisasjonsnummer = stringOrNull("arrangor_organisasjonsnummer"),
    )
}
