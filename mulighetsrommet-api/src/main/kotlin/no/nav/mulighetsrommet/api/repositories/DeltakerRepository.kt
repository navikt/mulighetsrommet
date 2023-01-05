package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.HistorikkDbo
import no.nav.mulighetsrommet.domain.dto.Deltakerstatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(deltaker: HistorikkDbo): QueryResult<HistorikkDbo> = query {
        logger.info("Lagrer deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into historikk (id, tiltaksgjennomforing_id, norsk_ident, status, fra_dato, til_dato, beskrivelse, virksomhetsnummer, tiltakstypeid)
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

        queryOf(query, deltaker.toSqlParameters())
            .map { it.toHistorikkDbo() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        logger.info("Sletter deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from deltaker
            where id = ?::uuid
        """.trimIndent()

        run { queryOf(query, id) }
            .asExecute
            .let { db.run(it) }
    }

    private fun HistorikkDbo.toSqlParameters(): Map<String, *> {
        return mapOf(
            "id" to id,
            "norsk_ident" to norskIdent,
            "status" to status.name,
            "fra_dato" to fraDato,
            "til_dato" to tilDato
        ) + when (this) {
            is HistorikkDbo.Gruppetiltak -> listOfNotNull(
                "tiltaksgjennomforing_id" to tiltaksgjennomforingId
            )
            is HistorikkDbo.IndividueltTiltak -> listOfNotNull(
                "beskrivelse" to beskrivelse,
                "virksomhetsnummer" to virksomhetsnummer,
                "tiltakstypeid" to tiltakstypeId
            )
        }
    }

    private fun Row.toHistorikkDbo(): HistorikkDbo {
        return when (uuidOrNull("tiltaksgjennomforing_id")) {
            null -> HistorikkDbo.IndividueltTiltak(
                id = uuid("id"),
                norskIdent = string("norsk_ident"),
                status = Deltakerstatus.valueOf(string("status")),
                fraDato = localDateTimeOrNull("fra_dato"),
                tilDato = localDateTimeOrNull("til_dato"),
                beskrivelse = string("beskrivelse"),
                tiltakstypeId = uuid("tiltakstypeid"),
                virksomhetsnummer = string("virksomhetsnummer")
            )
            else -> HistorikkDbo.Gruppetiltak(
                id = uuid("id"),
                tiltaksgjennomforingId = uuid("tiltaksgjennomforing_id"),
                norskIdent = string("norsk_ident"),
                status = Deltakerstatus.valueOf(string("status")),
                fraDato = localDateTimeOrNull("fra_dato"),
                tilDato = localDateTimeOrNull("til_dato")
            )
        }
    }
}
