package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.api.domain.dto.Innholdselement
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeEksternDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.*
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.PersonopplysningMedFrekvens
import no.nav.mulighetsrommet.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class TiltakstypeRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltakstype: TiltakstypeDbo): TiltakstypeDbo {
        logger.info("Lagrer tiltakstype id=${tiltakstype.id}")

        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (
                id,
                navn,
                tiltakskode,
                arena_kode,
                registrert_dato_i_arena,
                sist_endret_dato_i_arena,
                start_dato,
                slutt_dato,
                rett_paa_tiltakspenger
            )
            values (
                :id::uuid,
                :navn,
                :tiltakskode::tiltakskode,
                :arena_kode,
                :registrert_dato_i_arena,
                :sist_endret_dato_i_arena,
                :start_dato,
                :slutt_dato,
                :rett_paa_tiltakspenger
            )
            on conflict (id)
                do update set navn        = excluded.navn,
                              tiltakskode = excluded.tiltakskode,
                              arena_kode = excluded.arena_kode,
                              registrert_dato_i_arena = excluded.registrert_dato_i_arena,
                              sist_endret_dato_i_arena = excluded.sist_endret_dato_i_arena,
                              start_dato = excluded.start_dato,
                              slutt_dato = excluded.slutt_dato,
                              rett_paa_tiltakspenger = excluded.rett_paa_tiltakspenger
            returning *
        """.trimIndent()

        return queryOf(query, tiltakstype.toSqlParameters()).map { it.toTiltakstypeDbo() }.asSingle.let { db.run(it)!! }
    }

    fun get(id: UUID): TiltakstypeAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, id).map { it.toTiltakstypeAdminDto() }.asSingle
        return db.run(queryResult)
    }

    fun getEksternTiltakstype(id: UUID): TiltakstypeEksternDto? {
        val tiltakstype = get(id) ?: return null
        val tiltakskode = Tiltakskode.fromArenaKode(tiltakstype.arenaKode) ?: return null
        val deltakerRegistreringInnhold = getDeltakerregistreringInnholdByArenaKode(tiltakskode)

        return TiltakstypeEksternDto(
            id = tiltakstype.id,
            navn = tiltakstype.navn,
            tiltakskode = tiltakskode,
            arenaKode = tiltakstype.arenaKode,
            registrertIArenaDato = tiltakstype.registrertIArenaDato,
            sistEndretIArenaDato = tiltakstype.sistEndretIArenaDato,
            startDato = tiltakstype.startDato,
            sluttDato = tiltakstype.sluttDato,
            rettPaaTiltakspenger = tiltakstype.rettPaaTiltakspenger,
            status = tiltakstype.status,
            deltakerRegistreringInnhold = deltakerRegistreringInnhold,
        )
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): TiltakstypeAdminDto {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where tiltakskode = ?::tiltakskode
        """.trimIndent()
        val queryResult = queryOf(query, tiltakskode.name).map { it.toTiltakstypeAdminDto() }.asSingle
        return requireNotNull(db.run(queryResult)) {
            "Det finnes ingen tiltakstype for tiltakskode $tiltakskode"
        }
    }

    fun getBySanityId(sanityId: UUID): TiltakstypeAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where sanity_id = ?::uuid
        """.trimIndent()
        val queryResult = queryOf(query, sanityId).map { it.toTiltakstypeAdminDto() }.asSingle
        return db.run(queryResult)
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
    ): PaginatedResult<TiltakstypeAdminDto> {
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
                .mapPaginated { it.toTiltakstypeAdminDto() }
                .runWithSession(session)
        }
    }

    fun getAllSkalMigreres(
        pagination: Pagination = Pagination.all(),
        statuser: List<TiltakstypeStatus> = emptyList(),
        sortering: String? = null,
    ): PaginatedResult<TiltakstypeAdminDto> {
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
                .mapPaginated { it.toTiltakstypeAdminDto() }
                .runWithSession(session)
        }
    }

    private fun getDeltakerregistreringInnholdByArenaKode(tiltakskode: Tiltakskode): DeltakerRegistreringInnholdDto? {
        @Language("PostgreSQL")
        val query = """
           select dri.innholdskode, tekst, tt.deltaker_registrering_ledetekst
           from tiltakstype tt
               left join tiltakstype_deltaker_registrering_innholdselement tdri on tdri.tiltakskode = tt.tiltakskode
               left join deltaker_registrering_innholdselement dri on tdri.innholdskode = dri.innholdskode
           where tt.tiltakskode = ?::tiltakskode and tt.deltaker_registrering_ledetekst is not null;
        """.trimIndent()

        val result = queryOf(query, tiltakskode.name)
            .map {
                val innholdskode = it.stringOrNull("innholdskode")
                val tekst = it.stringOrNull("tekst")
                val ledetekst = it.string("deltaker_registrering_ledetekst")
                val innholdselement = if (tekst != null && innholdskode != null) {
                    Innholdselement(
                        tekst = tekst,
                        innholdskode = innholdskode,
                    )
                } else {
                    null
                }
                Pair(
                    ledetekst,
                    innholdselement,
                )
            }
            .asList
            .let { db.run(it) }

        if (result.isEmpty()) return null

        val innholdselementer = result.mapNotNull { it.second }
        return DeltakerRegistreringInnholdDto(
            ledetekst = result[0].first,
            innholdselementer = innholdselementer,
        )
    }

    fun getAllByDateInterval(
        dateIntervalStart: LocalDate,
        dateIntervalEnd: LocalDate,
    ): List<TiltakstypeAdminDto> {
        logger.info("Henter alle tiltakstyper med start- eller sluttdato mellom $dateIntervalStart og $dateIntervalEnd")

        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype_admin_dto_view
            where
                (start_dato > :date_interval_start and start_dato <= :date_interval_end) or
                (slutt_dato >= :date_interval_start and slutt_dato < :date_interval_end)
        """.trimIndent()

        return queryOf(
            query,
            mapOf(
                "date_interval_start" to dateIntervalStart,
                "date_interval_end" to dateIntervalEnd,
            ),
        ).map { it.toTiltakstypeAdminDto() }.asList.let { db.run(it) }
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
        "tiltakskode" to Tiltakskode.fromArenaKode(arenaKode)?.name,
        "arena_kode" to arenaKode,
        "registrert_dato_i_arena" to registrertDatoIArena,
        "sist_endret_dato_i_arena" to sistEndretDatoIArena,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "rett_paa_tiltakspenger" to rettPaaTiltakspenger,
    )

    private fun Row.toTiltakstypeDbo(): TiltakstypeDbo {
        return TiltakstypeDbo(
            id = uuid("id"),
            navn = string("navn"),
            arenaKode = string("arena_kode"),
            registrertDatoIArena = localDateTime("registrert_dato_i_arena"),
            sistEndretDatoIArena = localDateTime("sist_endret_dato_i_arena"),
            startDato = localDate("start_dato"),
            sluttDato = localDateOrNull("slutt_dato"),
            rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
        )
    }

    private fun Row.toTiltakstypeAdminDto(): TiltakstypeAdminDto {
        val personopplysninger = Json.decodeFromString<List<PersonopplysningMedFrekvens>>(string("personopplysninger"))
            .groupBy({ it.frekvens }, { it.personopplysning.toPersonopplysningMedBeskrivelse() })

        return TiltakstypeAdminDto(
            id = uuid("id"),
            navn = string("navn"),
            arenaKode = string("arena_kode"),
            registrertIArenaDato = localDateTime("registrert_dato_i_arena"),
            sistEndretIArenaDato = localDateTime("sist_endret_dato_i_arena"),
            startDato = localDate("start_dato"),
            sluttDato = localDateOrNull("slutt_dato"),
            sanityId = uuidOrNull("sanity_id"),
            rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
            status = TiltakstypeStatus.valueOf(string("status")),
            personopplysninger = personopplysninger,
        )
    }
}
