package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.utils.AvtaleFilter
import no.nav.mulighetsrommet.api.utils.DatabaseUtils
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class AvtaleRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(avtale: AvtaleDbo) = db.transaction { upsert(avtale, it) }

    fun upsert(avtale: AvtaleDbo, tx: Session) {
        logger.info("Lagrer avtale id=${avtale.id}")

        @Language("PostgreSQL")
        val query = """
            insert into avtale(id,
                               navn,
                               tiltakstype_id,
                               avtalenummer,
                               leverandor_organisasjonsnummer,
                               leverandor_kontaktperson_id,
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
                    :leverandor_kontaktperson_id,
                    :start_dato,
                    :slutt_dato,
                    :arena_ansvarlig_enhet,
                    :nav_region,
                    :avtaletype::avtaletype,
                    :avslutningsstatus::avslutningsstatus,
                    :prisbetingelser,
                    :antall_plasser,
                    :url,
                    :opphav::opphav)
            on conflict (id) do update set navn                           = excluded.navn,
                                           tiltakstype_id                 = excluded.tiltakstype_id,
                                           avtalenummer                   = excluded.avtalenummer,
                                           leverandor_organisasjonsnummer = excluded.leverandor_organisasjonsnummer,
                                           leverandor_kontaktperson_id    = excluded.leverandor_kontaktperson_id,
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
        val upsertAdministrator = """
             insert into avtale_administrator(avtale_id, nav_ident)
             values (?::uuid, ?)
             on conflict (avtale_id, nav_ident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAdministratorer = """
             delete from avtale_administrator
             where avtale_id = ?::uuid and not (nav_ident = any (?))
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

        @Language("PostgreSQL")
        val upsertUnderenheter = """
             insert into avtale_underleverandor (avtale_id, organisasjonsnummer)
             values (?::uuid, ?)
             on conflict (avtale_id, organisasjonsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUnderenheter = """
             delete from avtale_underleverandor
             where avtale_id = ?::uuid and not (organisasjonsnummer = any (?))
        """.trimIndent()

        tx.run(queryOf(query, avtale.toSqlParameters()).asExecute)

        avtale.administratorer.forEach { administrator ->
            queryOf(
                upsertAdministrator,
                avtale.id,
                administrator,
            ).asExecute.let { tx.run(it) }
        }

        queryOf(
            deleteAdministratorer,
            avtale.id,
            db.createTextArray(avtale.administratorer),
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

        avtale.leverandorUnderenheter.forEach { underenhet ->
            queryOf(
                upsertUnderenheter,
                avtale.id,
                underenhet,
            ).asExecute.let { tx.run(it) }
        }

        queryOf(
            deleteUnderenheter,
            avtale.id,
            db.createTextArray(avtale.leverandorUnderenheter),
        ).asExecute.let { tx.run(it) }
    }

    fun upsertArenaAvtale(avtale: ArenaAvtaleDbo) {
        logger.info("Lagrer avtale fra Arena id=${avtale.id}")

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
                               avtaletype,
                               avslutningsstatus,
                               prisbetingelser,
                               opphav)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :leverandor_organisasjonsnummer,
                    :start_dato,
                    :slutt_dato,
                    :arena_ansvarlig_enhet,
                    :avtaletype::avtaletype,
                    :avslutningsstatus::avslutningsstatus,
                    :prisbetingelser,
                    :opphav::opphav
                    )
            on conflict (id) do update set navn                           = excluded.navn,
                                           tiltakstype_id                 = excluded.tiltakstype_id,
                                           avtalenummer                   = excluded.avtalenummer,
                                           leverandor_organisasjonsnummer = excluded.leverandor_organisasjonsnummer,
                                           start_dato                     = excluded.start_dato,
                                           slutt_dato                     = excluded.slutt_dato,
                                           arena_ansvarlig_enhet          = excluded.arena_ansvarlig_enhet,
                                           avtaletype                     = excluded.avtaletype,
                                           avslutningsstatus              = excluded.avslutningsstatus,
                                           prisbetingelser                = excluded.prisbetingelser,
                                           antall_plasser                 = excluded.antall_plasser,
                                           opphav                         = excluded.opphav
            returning *
        """.trimIndent()

        queryOf(query, avtale.toSqlParameters()).asExecute.let { db.run(it) }
    }

    fun get(id: UUID): AvtaleAdminDto? = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): AvtaleAdminDto? {
        @Language("PostgreSQL")
        val query = """
            select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   vk.id                  as leverandor_kontaktperson_id,
                   vk.organisasjonsnummer as leverandor_kontaktperson_organisasjonsnummer,
                   vk.navn                as leverandor_kontaktperson_navn,
                   vk.telefon             as leverandor_kontaktperson_telefon,
                   vk.epost               as leverandor_kontaktperson_epost,
                   vk.beskrivelse         as leverandor_kontaktperson_beskrivelse,
                   v.navn                 as leverandor_navn,
                   a.start_dato,
                   a.slutt_dato,
                   a.opphav,
                   a.nav_region,
                   a.avtaletype,
                   a.avslutningsstatus,
                   a.prisbetingelser,
                   a.antall_plasser,
                   a.url,
                   nav_enhet.navn         as nav_enhet_navn,
                   t.navn                 as tiltakstype_navn,
                   t.tiltakskode,
                   an.nav_enheter,
                   au.leverandor_underenheter,
                   jsonb_agg(
                           distinct
                           case
                               when aa.nav_ident is null then null::jsonb
                               else jsonb_build_object('navIdent', aa.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn))
                               end
                       )                  as administratorer
            from avtale a
                join tiltakstype t on t.id = a.tiltakstype_id
                left join avtale_administrator aa on a.id = aa.avtale_id
                left join nav_ansatt na on na.nav_ident = aa.nav_ident
                left join nav_enhet on a.nav_region = nav_enhet.enhetsnummer
                left join lateral (
                    SELECT an.avtale_id, jsonb_strip_nulls(jsonb_agg(jsonb_build_object('enhetsnummer', an.enhetsnummer, 'navn', ne.navn))) as nav_enheter
                    FROM avtale_nav_enhet an left join nav_enhet ne on ne.enhetsnummer = an.enhetsnummer WHERE an.avtale_id = a.id GROUP BY 1
                ) an on true
                left join lateral (
                    SELECT au.avtale_id, jsonb_strip_nulls(jsonb_agg(jsonb_build_object('organisasjonsnummer', au.organisasjonsnummer, 'navn', v.navn))) as leverandor_underenheter
                    FROM avtale_underleverandor au inner join virksomhet v on v.organisasjonsnummer = au.organisasjonsnummer WHERE au.avtale_id = a.id GROUP BY 1
                ) au on true
                left join virksomhet v on v.organisasjonsnummer = a.leverandor_organisasjonsnummer
                left join virksomhet_kontaktperson vk on vk.id = a.leverandor_kontaktperson_id
            where a.id = ?::uuid
            group by a.id, t.navn, t.tiltakskode, nav_enhet.navn, v.navn, au.leverandor_underenheter, an.nav_enheter, vk.id
        """.trimIndent()

        return tx.run(
            queryOf(query, id)
                .map { it.toAvtaleAdminDto() }
                .asSingle,
        )
    }

    fun delete(id: UUID) {
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
            "leverandorOrgnr" to filter.leverandorOrgnr,
            "administrator_nav_ident" to filter.administratorNavIdent,
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            filter.tiltakstypeId to "a.tiltakstype_id = :tiltakstype_id",
            filter.search to "(lower(a.navn) like lower(:search))",
            filter.avtalestatus to filter.avtalestatus?.toDbStatement(),
            filter.navRegion to "(lower(a.nav_region) = lower(:nav_region) or lower(a.arena_ansvarlig_enhet) = lower(:nav_region) or lower(a.arena_ansvarlig_enhet) in (select enhetsnummer from nav_enhet where overordnet_enhet = :nav_region))",
            filter.leverandorOrgnr to "a.leverandor_organisasjonsnummer = :leverandorOrgnr",
            filter.administratorNavIdent to "aa.nav_ident = :administrator_nav_ident",
        )

        val order = when (filter.sortering) {
            "navn-ascending" -> "a.navn asc"
            "navn-descending" -> "a.navn desc"
            "leverandor-ascending" -> "v.navn asc"
            "leverandor-descending" -> "v.navn desc"
            "nav-enhet-ascending" -> "nav_enhet_navn asc"
            "nav-enhet-descending" -> "nav_enhet_navn desc"
            "startdato-ascending" -> "a.start_dato asc, a.navn asc"
            "startdato-descending" -> "a.start_dato desc, a.navn asc"
            "sluttdato-ascending" -> "a.slutt_dato asc, a.navn asc"
            "sluttdato-descending" -> "a.slutt_dato desc, a.navn asc"
            "tiltakstype_navn-ascending" -> "tiltakstype_navn asc, a.navn asc"
            "tiltakstype_navn-descending" -> "tiltakstype_navn desc, a.navn desc"
            else -> "a.navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select a.id,
                   a.navn,
                   a.tiltakstype_id,
                   a.avtalenummer,
                   a.leverandor_organisasjonsnummer,
                   vk.id                  as leverandor_kontaktperson_id,
                   vk.organisasjonsnummer as leverandor_kontaktperson_organisasjonsnummer,
                   vk.navn                as leverandor_kontaktperson_navn,
                   vk.telefon             as leverandor_kontaktperson_telefon,
                   vk.epost               as leverandor_kontaktperson_epost,
                   vk.beskrivelse         as leverandor_kontaktperson_beskrivelse,
                   v.navn                 as leverandor_navn,
                   a.start_dato,
                   a.slutt_dato,
                   a.opphav,
                   a.nav_region,
                   a.avtaletype,
                   a.avslutningsstatus,
                   a.prisbetingelser,
                   a.antall_plasser,
                   a.url,
                   nav_enhet.navn         as nav_enhet_navn,
                   t.navn                 as tiltakstype_navn,
                   t.tiltakskode,
                   an.nav_enheter,
                   au.leverandor_underenheter,
                   jsonb_agg(
                           distinct
                           case
                               when aa.nav_ident is null then null::jsonb
                               else jsonb_build_object('navIdent', aa.nav_ident, 'navn', concat(na.fornavn, ' ', na.etternavn))
                               end
                       )                  as administratorer,
                   count(*) over ()       as full_count
            from avtale a
                     join tiltakstype t on a.tiltakstype_id = t.id
                     left join nav_enhet on a.nav_region = nav_enhet.enhetsnummer
                     left join avtale_administrator aa on a.id = aa.avtale_id
                     left join nav_ansatt na on na.nav_ident = aa.nav_ident
                     left join avtale_nav_enhet ae on ae.avtale_id = a.id
                     left join avtale_underleverandor lva on lva.avtale_id = a.id
                     left join virksomhet v on v.organisasjonsnummer = a.leverandor_organisasjonsnummer
                     left join lateral (
                SELECT an.avtale_id, jsonb_strip_nulls(jsonb_agg(jsonb_build_object('enhetsnummer', an.enhetsnummer, 'navn', ne.navn))) as nav_enheter
                FROM avtale_nav_enhet an left join nav_enhet ne on ne.enhetsnummer = an.enhetsnummer WHERE an.avtale_id = a.id GROUP BY 1
                ) an on true
                     left join lateral (
                SELECT au.avtale_id, jsonb_strip_nulls(jsonb_agg(jsonb_build_object('organisasjonsnummer', au.organisasjonsnummer, 'navn', v.navn))) as leverandor_underenheter
                FROM avtale_underleverandor au left join virksomhet v on v.organisasjonsnummer = au.organisasjonsnummer WHERE au.avtale_id = a.id GROUP BY 1
                ) au on true
                     left join virksomhet_kontaktperson vk on vk.id = a.leverandor_kontaktperson_id
            $where
            group by a.id, t.navn, t.tiltakskode, nav_enhet.navn, v.navn, au.leverandor_underenheter, an.nav_enheter, vk.id
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
        "leverandor_kontaktperson_id" to leverandorKontaktpersonId,
        "leverandor_underenheter" to db.createTextArray(leverandorUnderenheter),
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

    private fun ArenaAvtaleDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "leverandor_organisasjonsnummer" to leverandorOrganisasjonsnummer,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        "avtaletype" to avtaletype.name,
        "avslutningsstatus" to avslutningsstatus.name,
        "prisbetingelser" to prisbetingelser,
        "opphav" to opphav.name,
    )

    private fun Row.toAvtaleAdminDto(): AvtaleAdminDto {
        val startDato = localDate("start_dato")
        val sluttDato = localDate("slutt_dato")
        val navRegion = stringOrNull("nav_region")
        val navEnheter = stringOrNull("nav_enheter")
            ?.let { Json.decodeFromString<List<NavEnhet?>>(it).filterNotNull() }
            ?: emptyList()
        val underenheter = stringOrNull("leverandor_underenheter")
            ?.let { Json.decodeFromString<List<AvtaleAdminDto.LeverandorUnderenhet?>>(it).filterNotNull() }
            ?: emptyList()
        val administratorer = Json
            .decodeFromString<List<AvtaleAdminDto.Administrator?>>(string("administratorer"))
            .filterNotNull()

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
                navn = stringOrNull("leverandor_navn"),
                slettet = stringOrNull("leverandor_navn") == null,
            ),
            leverandorUnderenheter = underenheter,
            leverandorKontaktperson = uuidOrNull("leverandor_kontaktperson_id")?.let {
                VirksomhetKontaktperson(
                    id = it,
                    organisasjonsnummer = string("leverandor_kontaktperson_organisasjonsnummer"),
                    navn = string("leverandor_kontaktperson_navn"),
                    telefon = stringOrNull("leverandor_kontaktperson_telefon"),
                    epost = string("leverandor_kontaktperson_epost"),
                    beskrivelse = stringOrNull("leverandor_kontaktperson_beskrivelse"),
                )
            },
            navEnheter = navEnheter,
            startDato = startDato,
            sluttDato = sluttDato,
            navRegion = navRegion?.let {
                NavEnhet(
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
            administrator = administratorer.getOrNull(0),
            url = stringOrNull("url"),
            antallPlasser = intOrNull("antall_plasser"),
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
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

    fun getAllAvtalerSomNarmerSegSluttdato(currentDate: LocalDate = LocalDate.now()): List<AvtaleNotificationDto> {
        val params = mapOf(
            "currentDate" to currentDate,
        )

        @Language("PostgreSQL")
        val query = """
            select a.id::uuid, a.navn, a.start_dato, a.slutt_dato, array_agg(distinct aa.nav_ident) as administratorer
            from avtale a
                     left join avtale_administrator aa on a.id = aa.avtale_id
            where (:currentDate::timestamp + interval '6' month) = a.slutt_dato
               or (:currentDate::timestamp + interval '3' month) = a.slutt_dato
               or (:currentDate::timestamp + interval '14' day) = a.slutt_dato
               or (:currentDate::timestamp + interval '7' day) = a.slutt_dato
            group by a.id, aa.nav_ident
        """.trimIndent()

        return queryOf(query, params)
            .map { it.toAvtaleNotificationDto() }
            .asList
            .let { db.run(it) }
    }

    private fun Row.toAvtaleNotificationDto(): AvtaleNotificationDto {
        val administratorer = arrayOrNull<String?>("administratorer")?.asList()?.filterNotNull() ?: emptyList()
        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")

        return AvtaleNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            startDato = startDato,
            sluttDato = sluttDato,
            administratorer = administratorer,
        )
    }

    fun avbrytAvtale(avtaleId: UUID) {
        logger.info("Avbryter avtale med id: '$avtaleId'")
        @Language("PostgreSQL")
        val patchAvslutningsstatusQuery = """
            update avtale
            set avslutningsstatus = 'AVBRUTT'
            where id::uuid = ?
        """.trimIndent()

        queryOf(patchAvslutningsstatusQuery, avtaleId).asUpdate.let { db.run(it) }
    }
}
