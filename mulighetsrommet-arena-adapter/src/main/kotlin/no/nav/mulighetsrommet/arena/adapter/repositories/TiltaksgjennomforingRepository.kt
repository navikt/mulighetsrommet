package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltaksgjennomforingRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltak: Tiltaksgjennomforing) = query {
        logger.info("Lagrer tiltaksgjennomføring id=${tiltak.id}, tiltakskode=${tiltak.tiltakskode}, sakId=${tiltak.sakId}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltaksgjennomforing (id, sanity_id, tiltaksgjennomforing_id, sak_id, tiltakskode, arrangor_id, navn, fra_dato, til_dato, apent_for_innsok, antall_plasser, status, avtale_id, deltidsprosent)
            values (:id::uuid, :sanity_id::uuid, :tiltaksgjennomforing_id, :sak_id, :tiltakskode, :arrangor_id, :navn, :fra_dato, :til_dato, :apent_for_innsok, :antall_plasser, :status, :avtale_id, :deltidsprosent)
            on conflict (id)
                do update set tiltaksgjennomforing_id = excluded.tiltaksgjennomforing_id,
                              sak_id                  = excluded.sak_id,
                              tiltakskode             = excluded.tiltakskode,
                              arrangor_id             = excluded.arrangor_id,
                              navn                    = excluded.navn,
                              fra_dato                = excluded.fra_dato,
                              til_dato                = excluded.til_dato,
                              apent_for_innsok        = excluded.apent_for_innsok,
                              antall_plasser          = excluded.antall_plasser,
                              status                  = excluded.status,
                              avtale_id               = excluded.avtale_id,
                              deltidsprosent          = excluded.deltidsprosent,
                              sanity_id               = excluded.sanity_id
            returning *
        """.trimIndent()

        queryOf(query, tiltak.toSqlParameters())
            .map { it.toTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun upsertSanityId(id: UUID, sanityId: UUID) = query {
        logger.info("Lagrer sanityId for tiltaksgjennomføring id=$id, sanityId=$sanityId")

        @Language("PostgreSQL")
        val query = """ update tiltaksgjennomforing set sanity_id = :sanity_id::uuid where id = :id::uuid """.trimIndent()

        queryOf(query, mapOf("id" to id, "sanity_id" to sanityId))
            .asExecute
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            delete from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    fun get(id: UUID): Tiltaksgjennomforing? {
        logger.info("Henter tiltaksgjennomføring id=$id")

        @Language("PostgreSQL")
        val query = """
            select
                id,
                tiltaksgjennomforing_id,
                sanity_id,
                sak_id,
                tiltakskode,
                arrangor_id,
                navn,
                fra_dato,
                til_dato,
                apent_for_innsok,
                antall_plasser,
                status,
                avtale_id,
                deltidsprosent
            from tiltaksgjennomforing
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Tiltaksgjennomforing.toSqlParameters() = mapOf(
        "id" to id,
        "sanity_id" to sanityId,
        "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
        "navn" to navn,
        "sak_id" to sakId,
        "tiltakskode" to tiltakskode,
        "arrangor_id" to arrangorId,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "apent_for_innsok" to apentForInnsok,
        "antall_plasser" to antallPlasser,
        "status" to status,
        "avtale_id" to avtaleId,
        "deltidsprosent" to deltidsprosent,
    )

    private fun Row.toTiltaksgjennomforing() = Tiltaksgjennomforing(
        id = uuid("id"),
        tiltaksgjennomforingId = int("tiltaksgjennomforing_id"),
        sanityId = uuidOrNull("sanity_id"),
        navn = string("navn"),
        sakId = int("sak_id"),
        tiltakskode = string("tiltakskode"),
        arrangorId = int("arrangor_id"),
        fraDato = localDateTime("fra_dato"),
        tilDato = localDateTimeOrNull("til_dato"),
        apentForInnsok = boolean("apent_for_innsok"),
        antallPlasser = intOrNull("antall_plasser"),
        status = string("status"),
        avtaleId = intOrNull("avtale_id"),
        deltidsprosent = double("deltidsprosent"),
    )
}
