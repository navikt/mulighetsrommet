package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.TiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltakshistorikkRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakshistorikk: TiltakshistorikkDbo): QueryResult<TiltakshistorikkDbo> = query {
        logger.info("Lagrer tiltakshistorikk id=${tiltakshistorikk.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakshistorikk (id, tiltaksgjennomforing_id, norsk_ident, status, fra_dato, til_dato, beskrivelse, virksomhetsnummer, tiltakstypeid)
            values (:id::uuid, :tiltaksgjennomforing_id::uuid, :norsk_ident, :status::deltakerstatus, :fra_dato, :til_dato, :beskrivelse, :virksomhetsnummer, :tiltakstypeid::uuid)
            on conflict (id)
                do update set tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              norsk_ident             = excluded.norsk_ident,
                              status                  = excluded.status,
                              fra_dato                = excluded.fra_dato,
                              til_dato                = excluded.til_dato,
                              beskrivelse             = excluded.beskrivelse,
                              virksomhetsnummer       = excluded.virksomhetsnummer,
                              tiltakstypeid           = excluded.tiltakstypeid
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

    private fun TiltakshistorikkDbo.toSqlParameters(): Map<String, *> {
        return mapOf(
            "id" to id,
            "norsk_ident" to norskIdent,
            "status" to status.name,
            "fra_dato" to fraDato,
            "til_dato" to tilDato
        ) + when (this) {
            is TiltakshistorikkDbo.Gruppetiltak -> listOfNotNull(
                "tiltaksgjennomforing_id" to tiltaksgjennomforingId
            )
            is TiltakshistorikkDbo.IndividueltTiltak -> listOfNotNull(
                "beskrivelse" to beskrivelse,
                "virksomhetsnummer" to virksomhetsnummer,
                "tiltakstypeid" to tiltakstypeId
            )
        }
    }

    private fun Row.toTiltakshistorikkDbo(): TiltakshistorikkDbo {
        return uuidOrNull("tiltaksgjennomforing_id")?.let {
            TiltakshistorikkDbo.Gruppetiltak(
                id = uuid("id"),
                tiltaksgjennomforingId = it,
                norskIdent = string("norsk_ident"),
                status = Deltakerstatus.valueOf(string("status")),
                fraDato = localDateTimeOrNull("fra_dato"),
                tilDato = localDateTimeOrNull("til_dato")
            )
        } ?: TiltakshistorikkDbo.IndividueltTiltak(
            id = uuid("id"),
            norskIdent = string("norsk_ident"),
            status = Deltakerstatus.valueOf(string("status")),
            fraDato = localDateTimeOrNull("fra_dato"),
            tilDato = localDateTimeOrNull("til_dato"),
            beskrivelse = stringOrNull("beskrivelse"),
            tiltakstypeId = uuid("tiltakstypeid"),
            virksomhetsnummer = stringOrNull("virksomhetsnummer")
        )
    }
}
