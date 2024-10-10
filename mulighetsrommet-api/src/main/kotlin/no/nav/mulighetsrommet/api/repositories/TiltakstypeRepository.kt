package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.*
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.domain.dto.Innholdselement
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.TiltakstypeEksternV2Dto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class TiltakstypeRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakstype: TiltakstypeDbo) {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (
                id,
                navn,
                tiltakskode,
                arena_kode,
                start_dato,
                slutt_dato
            )
            values (
                :id::uuid,
                :navn,
                :tiltakskode::tiltakskode,
                :arena_kode,
                :start_dato,
                :slutt_dato
            )
            on conflict (id)
                do update set navn        = excluded.navn,
                              tiltakskode = excluded.tiltakskode,
                              arena_kode = excluded.arena_kode,
                              start_dato = excluded.start_dato,
                              slutt_dato = excluded.slutt_dato
        """.trimIndent()

        queryOf(query, tiltakstype.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun get(id: UUID): TiltakstypeDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, id).map { it.toTiltakstypeDto() }.asSingle
        return db.run(queryResult)
    }

    fun getEksternTiltakstype(id: UUID): TiltakstypeEksternV2Dto? = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, innsatsgrupper
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        val deltakerRegistreringInnhold = getDeltakerregistreringInnhold(id, session)

        queryOf(query, id)
            .map { it.tiltakstypeEksternDto(deltakerRegistreringInnhold) }
            .asSingle.runWithSession(session)
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): TiltakstypeDto {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where tiltakskode = ?::tiltakskode
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode.name).map { it.toTiltakstypeDto() }.asSingle
        return requireNotNull(db.run(queryResult)) {
            "Det finnes ingen tiltakstype for tiltakskode $tiltakskode"
        }
    }

    fun getByArenaTiltakskode(arenaTiltakskode: String): TiltakstypeDto {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where arena_kode = ?
        """.trimIndent()
        val queryResult = queryOf(query, arenaTiltakskode).map { it.toTiltakstypeDto() }.asSingle
        return requireNotNull(db.run(queryResult)) {
            "Det finnes ingen tiltakstype med arena_kode $arenaTiltakskode"
        }
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where sanity_id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, sanityId).map { it.toTiltakstypeDto() }.asSingle
        return requireNotNull(db.run(queryResult)) {
            "Det finnes ingen tiltakstype med sanity_id=$sanityId"
        }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): TiltakstypeDto {
        @Language("PostgreSQL")
        val query = """
            select t.*
            from tiltakstype_admin_dto_view t
            join tiltaksgjennomforing g on g.tiltakstype_id = t.id
            where g.id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, gjennomforingId).map { it.toTiltakstypeDto() }.asSingle
        return requireNotNull(db.run(queryResult)) {
            "Det finnes ingen tiltakstype for gjennomforing med id=$gjennomforingId"
        }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
    ): PaginatedResult<TiltakstypeDto> {
        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from tiltakstype_admin_dto_view
            order by navn asc
            limit :limit
            offset :offset
        """.trimIndent()

        return db.useSession { session ->
            queryOf(query, pagination.parameters)
                .mapPaginated { it.toTiltakstypeDto() }
                .runWithSession(session)
        }
    }

    fun getAllSkalMigreres(
        pagination: Pagination = Pagination.all(),
        statuser: List<TiltakstypeStatus> = emptyList(),
        sortering: String? = null,
    ): PaginatedResult<TiltakstypeDto> {
        val parameters = mapOf(
            "statuser" to statuser.ifEmpty { null }?.let { db.createArrayOf("text", statuser) },
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "startdato-ascending" -> "start_dato asc"
            "startdato-descending" -> "start_dato desc"
            "sluttdato-ascending" -> "slutt_dato asc"
            "sluttdato-descending" -> "slutt_dato desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as total_count
            from tiltakstype_admin_dto_view
            where tiltakskode is not null
              and (:statuser::text[] is null or status = any(:statuser))
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return db.useSession { session ->
            queryOf(query, parameters + pagination.parameters)
                .mapPaginated { it.toTiltakstypeDto() }
                .runWithSession(session)
        }
    }

    private fun getDeltakerregistreringInnhold(id: UUID, session: Session): DeltakerRegistreringInnholdDto? {
        @Language("PostgreSQL")
        val query = """
           select tiltakstype.deltaker_registrering_ledetekst, element.innholdskode, element.tekst
           from tiltakstype
               left join tiltakstype_deltaker_registrering_innholdselement tiltakstype_innholdselement on tiltakstype_innholdselement.tiltakskode = tiltakstype.tiltakskode
               left join deltaker_registrering_innholdselement element on tiltakstype_innholdselement.innholdskode = element.innholdskode
           where tiltakstype.id = ?::uuid and tiltakstype.deltaker_registrering_ledetekst is not null;
        """.trimIndent()

        val result = queryOf(query, id)
            .map {
                val ledetekst = it.string("deltaker_registrering_ledetekst")
                val tekst = it.stringOrNull("tekst")
                val innholdskode = it.stringOrNull("innholdskode")
                val innholdselement = if (tekst != null && innholdskode != null) {
                    Innholdselement(tekst = tekst, innholdskode = innholdskode)
                } else {
                    null
                }
                Pair(ledetekst, innholdselement)
            }
            .asList
            .runWithSession(session)

        if (result.isEmpty()) return null

        val innholdselementer = result.mapNotNull { it.second }
        return DeltakerRegistreringInnholdDto(
            ledetekst = result[0].first,
            innholdselementer = innholdselementer,
        )
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id).asUpdate.let { db.run(it) }
    }

    private fun TiltakstypeDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode?.name,
        "arena_kode" to arenaKode,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
    )

    private fun Row.toTiltakstypeDto(): TiltakstypeDto {
        val innsatsgrupper = arrayOrNull<String>("innsatsgrupper")
            ?.map { Innsatsgruppe.valueOf(it) }
            ?.toSet()
            ?: emptySet()

        return TiltakstypeDto(
            id = uuid("id"),
            navn = string("navn"),
            innsatsgrupper = innsatsgrupper,
            arenaKode = string("arena_kode"),
            tiltakskode = stringOrNull("tiltakskode")?.let { Tiltakskode.valueOf(it) },
            startDato = localDate("start_dato"),
            sluttDato = localDateOrNull("slutt_dato"),
            sanityId = uuidOrNull("sanity_id"),
            status = TiltakstypeStatus.valueOf(string("status")),
        )
    }

    private fun Row.tiltakstypeEksternDto(
        deltakerRegistreringInnhold: DeltakerRegistreringInnholdDto?,
    ): TiltakstypeEksternV2Dto {
        val innsatsgrupper = arrayOrNull<String>("innsatsgrupper")
            ?.map { Innsatsgruppe.valueOf(it) }
            ?.toSet()
            ?: emptySet()
        return TiltakstypeEksternV2Dto(
            id = uuid("id"),
            navn = string("navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            innsatsgrupper = innsatsgrupper,
            arenaKode = string("arena_kode"),
            deltakerRegistreringInnhold = deltakerRegistreringInnhold,
        )
    }
}
