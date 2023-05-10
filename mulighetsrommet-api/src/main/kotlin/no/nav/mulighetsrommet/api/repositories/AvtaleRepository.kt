package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class AvtaleRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(avtale: AvtaleDbo): QueryResult<Unit> = query {
        logger.info("Lagrer avtale id=${avtale.id}")

        @Language("PostgreSQL")
        val query = """
            insert into avtale(id,
                               navn,
                               tiltakstype_id,
                               avtalenummer,
                               leverandor_organisasjonsnummer,
                               start_dato,
                               slutt_dato,
                               arena_ansvarlig_enhet,
                               nav_region,
                               avtaletype,
                               avslutningsstatus,
                               prisbetingelser,
                               antall_plasser,
                               url,
                               opphav)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :leverandor_organisasjonsnummer,
                    :start_dato,
                    :slutt_dato,
                    :arena_ansvarlig_enhet,
                    :nav_region,
                    :avtaletype::avtaletype,
                    :avslutningsstatus::avslutningsstatus,
                    :prisbetingelser,
                    :antall_plasser,
                    :url,
                    :opphav::avtaleopphav)
            on conflict (id) do update set navn                           = excluded.navn,
                                           tiltakstype_id                 = excluded.tiltakstype_id,
                                           avtalenummer                   = excluded.avtalenummer,
                                           leverandor_organisasjonsnummer = excluded.leverandor_organisasjonsnummer,
                                           start_dato                     = excluded.start_dato,
                                           slutt_dato                     = excluded.slutt_dato,
                                           arena_ansvarlig_enhet          = excluded.arena_ansvarlig_enhet,
                                           nav_region                     = excluded.nav_region,
                                           avtaletype                     = excluded.avtaletype,
                                           avslutningsstatus              = excluded.avslutningsstatus,
                                           prisbetingelser                = excluded.prisbetingelser,
                                           antall_plasser                 = excluded.antall_plasser,
                                           url                            = excluded.url,
                                           opphav                         = excluded.opphav
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertAnsvarlig = """
             insert into avtale_ansvarlig (avtale_id, navident)
             values (?::uuid, ?)
             on conflict (avtale_id, navident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAnsvarlige = """
             delete from avtale_ansvarlig
             where avtale_id = ?::uuid and not (navident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
             insert into avtale_nav_enhet (avtale_id, enhetsnummer)
             values (?::uuid, ?)
             on conflict (avtale_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
             delete from avtale_nav_enhet
             where avtale_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, avtale.toSqlParameters()).asExecute)

            avtale.ansvarlige.forEach { ansvarlig ->
                queryOf(
                    upsertAnsvarlig,
                    avtale.id,
                    ansvarlig,
                ).asExecute.let { tx.run(it) }
            }

            queryOf(
                deleteAnsvarlige,
                avtale.id,
                db.createTextArray(avtale.ansvarlige),
            ).asExecute.let { tx.run(it) }

            avtale.navEnheter.forEach { enhet ->
                queryOf(
                    upsertEnhet,
                    avtale.id,
                    enhet,
                ).asExecute.let { tx.run(it) }
            }

            queryOf(
                deleteEnheter,
                avtale.id,
                db.createTextArray(avtale.navEnheter),
            ).asExecute.let { tx.run(it) }
        }
    }

    fun get(id: UUID): QueryResult<AvtaleAdminDto?> = query {
        @Language("PostgreSQL")
        val query = """
            select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.nav_region,
                   a.avtaletype,
                   a.avslutningsstatus,
                   a.prisbetingelser,
                   a.url,
                   a.antall_plasser,
                   nav_enhet.navn as nav_enhet_navn,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode,
                   array_agg(e.enhetsnummer) as navEnheter,
                   aa.navident
            from avtale a
                     join tiltakstype t on t.id = a.tiltakstype_id
                     left join avtale_ansvarlig aa on a.id = aa.avtale_id
                     left join nav_enhet on a.nav_region = nav_enhet.enhetsnummer
                     left join avtale_nav_enhet e on e.avtale_id = a.id
            where a.id = ?::uuid
            group by a.id, t.tiltakskode, t.navn, aa.navident, nav_enhet.navn
        """.trimIndent()
        queryOf(query, id)
            .map { it.toAvtaleAdminDto() }
            .asSingle
            .let { db.run(it) }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Sletter avtale id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from avtale
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getAll(
        filter: AvtaleFilter,
        pagination: PaginationParams = PaginationParams(),
    ): Pair<Int, List<AvtaleAdminDto>> {
        if (filter.tiltakstypeId != null) {
            logger.info("Henter avtaler for tiltakstype med id: '${filter.tiltakstypeId}'")
        } else {
            logger.info("Henter alle avtaler")
        }
        val parameters = mapOf(
            "tiltakstype_id" to filter.tiltakstypeId,
            "search" to "%${filter.search}%",
            "nav_region" to filter.navRegion,
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "today" to filter.dagensDato,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.tiltakstypeId to "a.tiltakstype_id = :tiltakstype_id",
            filter.search to "(lower(a.navn) like lower(:search))",
            filter.avtalestatus to filter.avtalestatus?.toDbStatement(),
            filter.navRegion to "lower(a.nav_region) = lower(:nav_region) or lower(a.arena_ansvarlig_enhet) = lower(:nav_region) or lower(a.arena_ansvarlig_enhet) in (select enhetsnummer from nav_enhet where overordnet_enhet = :nav_region)",
        )

        val order = when (filter.sortering) {
            "navn-ascending" -> "a.navn asc"
            "navn-descending" -> "a.navn desc"
            "leverandor-ascending" -> "a.leverandor_organisasjonsnummer asc"
            "leverandor-descending" -> "a.leverandor_organisasjonsnummer desc"
            "enhet-ascending" -> "e.navn asc"
            "enhet-descending" -> "e.navn desc"
            "startdato-ascending" -> "a.start_dato asc, a.navn asc"
            "startdato-descending" -> "a.start_dato desc, a.navn asc"
            "sluttdato-ascending" -> "a.slutt_dato asc, a.navn asc"
            "sluttdato-descending" -> "a.slutt_dato desc, a.navn asc"
            else -> "a.navn asc"
        }

        @Language("PostgreSQL")
        val query = """
           select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   a.start_dato,
                   a.slutt_dato,
                   a.nav_region,
                   a.avtaletype,
                   a.avslutningsstatus,
                   a.prisbetingelser,
                   a.antall_plasser,
                   a.url,
                   nav_enhet.navn as nav_enhet_navn,
                   t.navn as tiltakstype_navn,
                   t.tiltakskode,
                   aa.navident as navident,
                   array_agg(ae.enhetsnummer) as navEnheter,
                   count(*) over () as full_count
            from avtale a
                     join tiltakstype t on a.tiltakstype_id = t.id
                     left join nav_enhet on a.nav_region = nav_enhet.enhetsnummer
                     left join avtale_ansvarlig aa on a.id = aa.avtale_id
                     left join avtale_nav_enhet ae on ae.avtale_id = a.id
            $where
            group by a.id, t.navn, t.tiltakskode, aa.navident, nav_enhet.navn
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val results = queryOf(query, parameters)
            .map {
                it.int("full_count") to it.toAvtaleAdminDto()
            }
            .asList
            .let { db.run(it) }

        val totaltAntall = results.firstOrNull()?.first ?: 0
        val avtaler = results.map { it.second }
        return Pair(totaltAntall, avtaler)
    }

    private fun AvtaleDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "leverandor_organisasjonsnummer" to leverandorOrganisasjonsnummer,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        "nav_region" to navRegion,
        "avtaletype" to avtaletype.name,
        "avslutningsstatus" to avslutningsstatus.name,
        "prisbetingelser" to prisbetingelser,
        "antall_plasser" to antallPlasser,
        "url" to url,
        "opphav" to opphav.name,
    )

    @Suppress("UNCHECKED_CAST")
    private fun Row.toAvtaleAdminDto(): AvtaleAdminDto {
        val startDato = localDate("start_dato")
        val sluttDato = localDate("slutt_dato")
        val navRegion = stringOrNull("nav_region")
        val navEnheter = sqlArrayOrNull("navEnheter")?.let {
            (it.array as Array<String?>).asList().filterNotNull()
        } ?: emptyList()

        return AvtaleAdminDto(
            id = uuid("id"),
            navn = string("navn"),
            tiltakstype = AvtaleAdminDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                arenaKode = string("tiltakskode"),
            ),
            avtalenummer = stringOrNull("avtalenummer"),
            leverandor = AvtaleAdminDto.Leverandor(
                organisasjonsnummer = string("leverandor_organisasjonsnummer"),
            ),
            navEnheter = navEnheter,
            startDato = startDato,
            sluttDato = sluttDato,
            navRegion = navRegion?.let {
                AvtaleAdminDto.NavEnhet(
                    enhetsnummer = it,
                    navn = string("nav_enhet_navn"),
                )
            },
            avtaletype = Avtaletype.valueOf(string("avtaletype")),
            avtalestatus = Avtalestatus.resolveFromDatesAndAvslutningsstatus(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            prisbetingelser = stringOrNull("prisbetingelser"),
            ansvarlig = stringOrNull("navident"),
            url = stringOrNull("url"),
            antallPlasser = intOrNull("antall_plasser"),
        )
    }

    private fun Avtalestatus.toDbStatement(): String {
        return when (this) {
            Avtalestatus.Aktiv -> "(avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}' and (:today >= start_dato and :today <= slutt_dato))"
            Avtalestatus.Avsluttet -> "(avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}' or :today > slutt_dato)"
            Avtalestatus.Avbrutt -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
            Avtalestatus.Planlagt -> "(avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}' and :today < start_dato)"
        }
    }

    fun countAktiveAvtalerForTiltakstypeWithId(id: UUID, currentDate: LocalDate = LocalDate.now()): Int {
        val query = """
             SELECT count(id) AS antall
             FROM avtale
             WHERE tiltakstype_id = ?
             and start_dato < ?::timestamp
             and slutt_dato > ?::timestamp
        """.trimIndent()

        return queryOf(query, id, currentDate, currentDate)
            .map { it.int("antall") }
            .asSingle
            .let { db.run(it)!! }
    }

    fun countTiltaksgjennomforingerForAvtaleWithId(id: UUID, currentDate: LocalDate = LocalDate.now()): Int {
        val query = """
            select count(*) as antall
            from tiltaksgjennomforing
            where avtale_id::uuid = ?
            and start_dato < ?::timestamp
            and slutt_dato > ?::timestamp
        """.trimIndent()

        return queryOf(query, id, currentDate, currentDate)
            .map { it.int("antall") }
            .asSingle
            .let { db.run(it)!! }
    }
}
