package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dto.*
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
                               avtaletype,
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
                    :avtaletype::avtaletype,
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
                                           avtaletype                     = excluded.avtaletype,
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
            select *
            from avtale_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return tx.run(
            queryOf(query, id)
                .map { it.toAvtaleAdminDto() }
                .asSingle,
        )
    }

    fun getAll(
        pagination: PaginationParams = PaginationParams(),
        tiltakstypeId: UUID? = null,
        search: String? = null,
        status: Avtalestatus? = null,
        navRegion: String? = null,
        sortering: String? = null,
        dagensDato: LocalDate = LocalDate.now(),
        leverandorOrgnr: String? = null,
        administratorNavIdent: String? = null,
    ): Pair<Int, List<AvtaleAdminDto>> {
        val parameters = mapOf(
            "tiltakstype_id" to tiltakstypeId,
            "search" to "%${search?.replace("/", "#")?.trim()}%",
            "nav_region_json" to navRegion?.let { """[{"enhetsnummer":"$it"}]""" },
            "nav_region" to navRegion,
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "today" to dagensDato,
            "leverandorOrgnr" to leverandorOrgnr,
            "administrator_nav_ident" to administratorNavIdent?.let { """[{"navIdent": "$it" }]""" },
        )

        val where = DatabaseUtils.andWhereParameterNotNull(
            tiltakstypeId to "tiltakstype_id = :tiltakstype_id",
            search to "(lower(navn) like lower(:search)) or avtalenummer like :search",
            status to status?.toDbStatement(),
            navRegion to "(nav_enheter @> :nav_region_json::jsonb or arena_ansvarlig_enhet = :nav_region or arena_ansvarlig_enhet in (select enhetsnummer from nav_enhet where overordnet_enhet = :nav_region))",
            leverandorOrgnr to "leverandor_organisasjonsnummer = :leverandorOrgnr",
            administratorNavIdent to "administratorer @> :administrator_nav_ident::jsonb",
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "leverandor-ascending" -> "leverandor_navn asc"
            "leverandor-descending" -> "leverandor_navn desc"
            "startdato-ascending" -> "start_dato asc, navn asc"
            "startdato-descending" -> "start_dato desc, navn asc"
            "sluttdato-ascending" -> "slutt_dato asc, navn asc"
            "sluttdato-descending" -> "slutt_dato desc, navn asc"
            "tiltakstype_navn-ascending" -> "tiltakstype_navn asc, navn asc"
            "tiltakstype_navn-descending" -> "tiltakstype_navn desc, navn desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select *, count(*) over() as full_count
            from avtale_admin_dto_view
            $where
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
            group by a.id
        """.trimIndent()

        return queryOf(query, params)
            .map { it.toAvtaleNotificationDto() }
            .asList
            .let { db.run(it) }
    }

    fun setAvslutningsstatus(id: UUID, status: Avslutningsstatus) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set avslutningsstatus = :status::avslutningsstatus
            where id = :id::uuid
        """.trimIndent()

        queryOf(query, mapOf("id" to id, "status" to status.name))
            .asUpdate
            .let { db.run(it) }
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
        "avtaletype" to avtaletype.name,
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

        val underenheter = stringOrNull("leverandor_underenheter")
            ?.let { Json.decodeFromString<List<AvtaleAdminDto.LeverandorUnderenhet?>>(it).filterNotNull() }
            ?: emptyList()
        val administratorer = Json
            .decodeFromString<List<AvtaleAdminDto.Administrator?>>(string("administratorer"))
            .filterNotNull()

        val navEnheter = stringOrNull("nav_enheter")
            ?.let { Json.decodeFromString<List<EmbeddedNavEnhet?>>(it).filterNotNull() }
            ?: emptyList()
        val kontorstruktur = navEnheter
            .filter { it.type == Norg2Type.FYLKE }
            .map { region ->
                val kontorer = navEnheter
                    .filter { enhet -> enhet.type != Norg2Type.FYLKE && enhet.overordnetEnhet == region.enhetsnummer }
                    .sortedBy { enhet -> enhet.navn }
                Kontorstruktur(region = region, kontorer = kontorer)
            }

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
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = stringOrNull("arena_ansvarlig_enhet"),
            avtaletype = Avtaletype.valueOf(string("avtaletype")),
            avtalestatus = Avtalestatus.resolveFromDatesAndAvslutningsstatus(
                LocalDate.now(),
                startDato,
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            prisbetingelser = stringOrNull("prisbetingelser"),
            administratorer = administratorer,
            url = stringOrNull("url"),
            antallPlasser = intOrNull("antall_plasser"),
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            updatedAt = localDateTime("updated_at"),
            kontorstruktur = kontorstruktur,
        )
    }

    private fun Avtalestatus.toDbStatement(): String {
        return when (this) {
            Avtalestatus.Aktiv -> "(avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}' and (:today <= slutt_dato))"
            Avtalestatus.Avsluttet -> "(avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}' or :today > slutt_dato)"
            Avtalestatus.Avbrutt -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
        }
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
}
