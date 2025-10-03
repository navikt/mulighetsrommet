package no.nav.mulighetsrommet.api.tiltakstype.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.*
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.*

class TiltakstypeQueries(private val session: Session) {

    fun upsert(tiltakstype: TiltakstypeDbo) = with(session) {
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

        execute(queryOf(query, tiltakstype.toSqlParameters()))
    }

    fun get(id: UUID): TiltakstypeDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toTiltakstypeDto() }
    }

    fun getEksternTiltakstype(id: UUID): TiltakstypeEksternV2Dto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, innsatsgrupper, created_at, updated_at
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        val deltakerRegistreringInnhold = getDeltakerregistreringInnhold(id)

        return single(queryOf(query, id)) { it.tiltakstypeEksternDto(deltakerRegistreringInnhold) }
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): TiltakstypeDto = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where tiltakskode = ?::tiltakskode
        """.trimIndent()

        val tiltakstype = single(queryOf(query, tiltakskode.name)) { it.toTiltakstypeDto() }

        return requireNotNull(tiltakstype) {
            "Det finnes ingen tiltakstype for tiltakskode $tiltakskode"
        }
    }

    fun getByArenaTiltakskode(arenaTiltakskode: String): TiltakstypeDto = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where arena_kode = ?
        """.trimIndent()

        val tiltakstype = single(queryOf(query, arenaTiltakskode)) { it.toTiltakstypeDto() }

        return requireNotNull(tiltakstype) {
            "Det finnes ingen tiltakstype med arena_kode $arenaTiltakskode"
        }
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeDto = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where sanity_id = ?::uuid
        """.trimIndent()

        val tiltakstype = single(queryOf(query, sanityId)) { it.toTiltakstypeDto() }

        return requireNotNull(tiltakstype) {
            "Det finnes ingen tiltakstype med sanity_id=$sanityId"
        }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): TiltakstypeDto = with(session) {
        @Language("PostgreSQL")
        val query = """
            select t.*
            from tiltakstype_admin_dto_view t
            join gjennomforing g on g.tiltakstype_id = t.id
            where g.id = ?::uuid
        """.trimIndent()

        val tiltakstype = single(queryOf(query, gjennomforingId)) { it.toTiltakstypeDto() }

        return requireNotNull(tiltakstype) {
            "Det finnes ingen tiltakstype for gjennomforing med id=$gjennomforingId"
        }
    }

    fun getAll(
        tiltakskoder: Set<Tiltakskode> = setOf(),
        statuser: List<TiltakstypeStatus> = emptyList(),
        sortering: String? = null,
    ): List<TiltakstypeDto> = with(session) {
        val parameters = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { createArrayOfTiltakskode(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createTextArray(it) },
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
            where (:tiltakskoder::tiltakskode[] is null or tiltakskode = any(:tiltakskoder))
              and (:statuser::text[] is null or status = any(:statuser))
            order by $order
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toTiltakstypeDto() }
    }

    private fun getDeltakerregistreringInnhold(id: UUID): DeltakerRegistreringInnholdDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
           select tiltakstype.deltaker_registrering_ledetekst, element.innholdskode, element.tekst
           from tiltakstype
               left join tiltakstype_deltaker_registrering_innholdselement tiltakstype_innholdselement on tiltakstype_innholdselement.tiltakskode = tiltakstype.tiltakskode
               left join deltaker_registrering_innholdselement element on tiltakstype_innholdselement.innholdskode = element.innholdskode
           where tiltakstype.id = ?::uuid and tiltakstype.deltaker_registrering_ledetekst is not null;
        """.trimIndent()

        val result = list(queryOf(query, id)) {
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

        if (result.isEmpty()) return null

        val innholdselementer = result.mapNotNull { it.second }
        return DeltakerRegistreringInnholdDto(
            ledetekst = result[0].first,
            innholdselementer = innholdselementer,
        )
    }

    fun delete(id: UUID): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, id))
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
            opprettetTidspunkt = localDateTime("created_at"),
            oppdatertTidspunkt = localDateTime("updated_at"),
            deltakerRegistreringInnhold = deltakerRegistreringInnhold,
        )
    }
}

fun Session.createArrayOfTiltakskode(
    values: Collection<Tiltakskode>,
): Array = createArrayOf("tiltakskode", values)
