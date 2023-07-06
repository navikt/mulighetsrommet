package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltakshistorikkRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> = query {
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

        queryOf(query, tiltakshistorikk.toSqlParameters())
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

    fun getTiltakshistorikkForBruker(norskIdent: String): List<TiltakshistorikkDto> {
        @Language("PostgreSQL")
        val query = """
            select h.id,
                   h.fra_dato,
                   h.til_dato,
                   h.status,
                   coalesce(g.navn, h.beskrivelse) as navn,
                   coalesce(g.arrangor_organisasjonsnummer, h.arrangor_organisasjonsnummer) as arrangor_organisasjonsnummer,
                   t.navn as tiltakstype
            from tiltakshistorikk h
                     left join tiltaksgjennomforing g on g.id = h.tiltaksgjennomforing_id
                     left join tiltakstype t on t.id = coalesce(g.tiltakstype_id, h.tiltakstypeid)
            where h.norsk_ident = ?
            order by h.fra_dato desc nulls last;
        """.trimIndent()
        val queryResult = queryOf(query, norskIdent).map { it.toTiltakshistorikk() }.asList
        return db.run(queryResult)
    }

    private fun TiltakshistorikkDbo.toSqlParameters(): Map<String, *> {
        return mapOf(
            "id" to id,
            "norsk_ident" to norskIdent,
            "status" to status.name,
            "fra_dato" to fraDato,
            "til_dato" to tilDato,
            "registrert_i_arena_dato" to registrertIArenaDato,
        ) + when (this) {
            is TiltakshistorikkDbo.Gruppetiltak -> listOfNotNull(
                "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
            )

            is TiltakshistorikkDbo.IndividueltTiltak -> listOfNotNull(
                "beskrivelse" to beskrivelse,
                "arrangor_organisasjonsnummer" to arrangorOrganisasjonsnummer,
                "tiltakstypeid" to tiltakstypeId,
            )
        }
    }

    private fun Row.toTiltakshistorikkDbo(): TiltakshistorikkDbo {
        return uuidOrNull("tiltaksgjennomforing_id")
            ?.let {
                TiltakshistorikkDbo.Gruppetiltak(
                    id = uuid("id"),
                    tiltaksgjennomforingId = it,
                    norskIdent = string("norsk_ident"),
                    status = Deltakerstatus.valueOf(string("status")),
                    fraDato = localDateTimeOrNull("fra_dato"),
                    tilDato = localDateTimeOrNull("til_dato"),
                    registrertIArenaDato = localDateTime("registrert_i_arena_dato"),
                )
            }
            ?: TiltakshistorikkDbo.IndividueltTiltak(
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

    private fun Row.toTiltakshistorikk() = TiltakshistorikkDto(
        id = uuid("id"),
        fraDato = localDateTimeOrNull("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        status = Deltakerstatus.valueOf(string("status")),
        tiltaksnavn = stringOrNull("navn"),
        tiltakstype = string("tiltakstype"),
        arrangor = stringOrNull("arrangor_organisasjonsnummer")?.let {
            TiltakshistorikkDto.Arrangor(organisasjonsnummer = it, navn = null)
        },
    )
}
