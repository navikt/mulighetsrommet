package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotificationDto
import no.nav.mulighetsrommet.api.domain.dto.Kontorstruktur
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.NavIdent
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
                               arrangor_hovedenhet_id,
                               arrangor_kontaktperson_id,
                               start_dato,
                               slutt_dato,
                               avtaletype,
                               prisbetingelser,
                               antall_plasser,
                               url,
                               opphav,
                               beskrivelse,
                               faneinnhold)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :arrangor_hovedenhet_id,
                    :arrangor_kontaktperson_id,
                    :start_dato,
                    :slutt_dato,
                    :avtaletype::avtaletype,
                    :prisbetingelser,
                    :antall_plasser,
                    :url,
                    :opphav::opphav,
                    :beskrivelse,
                    :faneinnhold::jsonb)
            on conflict (id) do update set navn                        = excluded.navn,
                                           tiltakstype_id              = excluded.tiltakstype_id,
                                           avtalenummer                = excluded.avtalenummer,
                                           arrangor_hovedenhet_id      = excluded.arrangor_hovedenhet_id,
                                           arrangor_kontaktperson_id   = excluded.arrangor_kontaktperson_id,
                                           start_dato                  = excluded.start_dato,
                                           slutt_dato                  = excluded.slutt_dato,
                                           avtaletype                  = excluded.avtaletype,
                                           prisbetingelser             = excluded.prisbetingelser,
                                           antall_plasser              = excluded.antall_plasser,
                                           url                         = excluded.url,
                                           opphav                      = coalesce(avtale.opphav, excluded.opphav),
                                           beskrivelse                 = excluded.beskrivelse,
                                           faneinnhold                 = excluded.faneinnhold
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
        val deleteUnderenheter = """
             delete from avtale_arrangor_underenhet
             where avtale_id = ?::uuid and not (arrangor_id = any (?))
        """.trimIndent()

        tx.run(queryOf(query, avtale.toSqlParameters()).asExecute)

        avtale.administratorer.forEach { administrator ->
            queryOf(
                upsertAdministrator,
                avtale.id,
                administrator.value,
            ).asExecute.let { tx.run(it) }
        }

        queryOf(
            deleteAdministratorer,
            avtale.id,
            db.createTextArray(avtale.administratorer.map { it.value }),
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

        avtale.arrangorUnderenheter.forEach { underenhet ->
            setArrangorUnderenhet(tx, avtale.id, underenhet)
        }

        queryOf(
            deleteUnderenheter,
            avtale.id,
            db.createUuidArray(avtale.arrangorUnderenheter),
        ).asExecute.let { tx.run(it) }
    }

    fun upsertArenaAvtale(avtale: ArenaAvtaleDbo): Unit = db.transaction {
        upsertArenaAvtale(it, avtale)
    }

    fun upsertArenaAvtale(tx: Session, avtale: ArenaAvtaleDbo) {
        logger.info("Lagrer avtale fra Arena id=${avtale.id}")

        val arrangorId = queryOf(
            "select id from arrangor where organisasjonsnummer = ?",
            avtale.arrangorOrganisasjonsnummer,
        )
            .map { it.uuid("id") }
            .asSingle
            .let { requireNotNull(db.run(it)) }

        @Language("PostgreSQL")
        val query = """
            insert into avtale(id,
                               navn,
                               tiltakstype_id,
                               avtalenummer,
                               arrangor_hovedenhet_id,
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
                    :arrangor_hovedenhet_id,
                    :start_dato,
                    :slutt_dato,
                    :arena_ansvarlig_enhet,
                    :avtaletype::avtaletype,
                    :avslutningsstatus::avslutningsstatus,
                    :prisbetingelser,
                    :opphav::opphav)
            on conflict (id) do update set navn                     = excluded.navn,
                                           tiltakstype_id           = excluded.tiltakstype_id,
                                           avtalenummer             = excluded.avtalenummer,
                                           arrangor_hovedenhet_id   = excluded.arrangor_hovedenhet_id,
                                           start_dato               = excluded.start_dato,
                                           slutt_dato               = excluded.slutt_dato,
                                           arena_ansvarlig_enhet    = excluded.arena_ansvarlig_enhet,
                                           avtaletype               = excluded.avtaletype,
                                           avslutningsstatus        = excluded.avslutningsstatus,
                                           prisbetingelser          = excluded.prisbetingelser,
                                           antall_plasser           = excluded.antall_plasser,
                                           opphav                   = coalesce(avtale.opphav, excluded.opphav)
        """.trimIndent()

        queryOf(query, avtale.toSqlParameters(arrangorId)).asExecute.let { tx.run(it) }
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
        tiltakstypeIder: List<UUID> = emptyList(),
        search: String? = null,
        statuser: List<Avtalestatus> = emptyList(),
        avtaletyper: List<Avtaletype> = emptyList(),
        navRegioner: List<String> = emptyList(),
        sortering: String? = null,
        dagensDato: LocalDate = LocalDate.now(),
        arrangorIds: List<UUID> = emptyList(),
        administratorNavIdent: NavIdent? = null,
    ): Pair<Int, List<AvtaleAdminDto>> {
        val parameters = mapOf(
            "today" to dagensDato,
            "search" to search?.replace("/", "#")?.trim()?.let { "%$it%" },
            "limit" to pagination.limit,
            "offset" to pagination.offset,
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { db.createUuidArray(it) },
            "nav_enheter" to navRegioner.ifEmpty { null }?.let { db.createTextArray(it) },
            "avtaletyper" to avtaletyper.ifEmpty { null }?.let { db.createArrayOf("avtaletype", it) },
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "arrangor-ascending" -> "arrangor_hovedenhet_navn asc"
            "arrangor-descending" -> "arrangor_hovedenhet_navn desc"
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
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any (:tiltakstype_ids))
              and (:search::text is null or (navn ilike :search or avtalenummer ilike :search or arrangor_hovedenhet_navn ilike :search))
              and (:nav_enheter::text[] is null or (
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   arena_nav_enhet_enhetsnummer = any (:nav_enheter) or
                   arena_nav_enhet_enhetsnummer in (select enhetsnummer
                                                    from nav_enhet
                                                    where overordnet_enhet = any (:nav_enheter))))
              and (:arrangor_hovedenhet_id::text is null or :arrangor_hovedenhet_id = any (:arrangor_ids))
              and (:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb)
              and (:avtaletyper::avtaletype[] is null or avtaletype = any (:avtaletyper))
              and (${statuserWhereStatement(statuser)})
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

    fun setOpphav(id: UUID, opphav: ArenaMigrering.Opphav) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set opphav = :opphav::opphav
            where id = :id::uuid
        """.trimIndent()

        queryOf(query, mapOf("id" to id, "opphav" to opphav.name))
            .asUpdate
            .let { db.run(it) }
    }

    fun setAvslutningsstatus(id: UUID, status: Avslutningsstatus) {
        db.transaction { setAvslutningsstatus(it, id, status) }
    }

    fun setAvslutningsstatus(tx: Session, id: UUID, status: Avslutningsstatus) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set avslutningsstatus = :status::avslutningsstatus
            where id = :id::uuid
        """.trimIndent()

        queryOf(query, mapOf("id" to id, "status" to status.name))
            .asUpdate
            .let { tx.run(it) }
    }

    fun setArrangorUnderenhet(tx: Session, avtaleId: UUID, arrangorId: UUID) {
        @Language("PostgreSQL")
        val query = """
             insert into avtale_arrangor_underenhet (avtale_id, arrangor_id)
             values (?::uuid, ?::uuid)
             on conflict (avtale_id, arrangor_id) do nothing
        """.trimIndent()

        queryOf(query, avtaleId, arrangorId)
            .asExecute
            .let { tx.run(it) }
    }

    fun delete(tx: Session, id: UUID) {
        logger.info("Sletter avtale id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from avtale
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { tx.run(it) }
    }

    private fun AvtaleDbo.toSqlParameters() = mapOf(
        "opphav" to ArenaMigrering.Opphav.MR_ADMIN_FLATE.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "arrangor_hovedenhet_id" to arrangorId,
        "arrangor_kontaktperson_id" to arrangorKontaktpersonId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "avtaletype" to avtaletype.name,
        "prisbetingelser" to prisbetingelser,
        "antall_plasser" to antallPlasser,
        "url" to url,
        "beskrivelse" to beskrivelse,
        "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
    )

    private fun ArenaAvtaleDbo.toSqlParameters(arrangorId: UUID) = mapOf(
        "opphav" to ArenaMigrering.Opphav.ARENA.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "arrangor_hovedenhet_id" to arrangorId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
        "avtaletype" to avtaletype.name,
        "avslutningsstatus" to avslutningsstatus.name,
        "prisbetingelser" to prisbetingelser,
    )

    private fun Row.toAvtaleAdminDto(): AvtaleAdminDto {
        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")

        val underenheter = stringOrNull("arrangor_underenheter")
            ?.let { Json.decodeFromString<List<AvtaleAdminDto.ArrangorUnderenhet?>>(it).filterNotNull() }
            ?: emptyList()
        val administratorer = Json
            .decodeFromString<List<AvtaleAdminDto.Administrator?>>(string("administratorer_json"))
            .filterNotNull()

        val navEnheter = stringOrNull("nav_enheter_json")
            ?.let { Json.decodeFromString<List<NavEnhetDbo?>>(it).filterNotNull() }
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
            avtalenummer = stringOrNull("avtalenummer"),
            startDato = startDato,
            sluttDato = sluttDato,
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            avtaletype = Avtaletype.valueOf(string("avtaletype")),
            avtalestatus = Avtalestatus.resolveFromDatesAndAvslutningsstatus(
                LocalDate.now(),
                sluttDato,
                Avslutningsstatus.valueOf(string("avslutningsstatus")),
            ),
            prisbetingelser = stringOrNull("prisbetingelser"),
            antallPlasser = intOrNull("antall_plasser"),
            url = stringOrNull("url"),
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            administratorer = administratorer,
            kontorstruktur = kontorstruktur,
            arrangor = AvtaleAdminDto.ArrangorHovedenhet(
                id = uuid("arrangor_hovedenhet_id"),
                organisasjonsnummer = string("arrangor_hovedenhet_organisasjonsnummer"),
                navn = string("arrangor_hovedenhet_navn"),
                slettet = boolean("arrangor_hovedenhet_slettet"),
                underenheter = underenheter,
                kontaktperson = uuidOrNull("arrangor_hovedenhet_kontaktperson_id")?.let {
                    ArrangorKontaktperson(
                        id = it,
                        arrangorId = uuid("arrangor_hovedenhet_kontaktperson_arrangor_id"),
                        navn = string("arrangor_hovedenhet_kontaktperson_navn"),
                        telefon = stringOrNull("arrangor_hovedenhet_kontaktperson_telefon"),
                        epost = string("arrangor_hovedenhet_kontaktperson_epost"),
                        beskrivelse = stringOrNull("arrangor_hovedenhet_kontaktperson_beskrivelse"),
                    )
                },
            ),
            arenaAnsvarligEnhet = stringOrNull("arena_nav_enhet_enhetsnummer")?.let {
                ArenaNavEnhet(
                    navn = stringOrNull("arena_nav_enhet_navn"),
                    enhetsnummer = it,
                )
            },
            tiltakstype = AvtaleAdminDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                arenaKode = string("tiltakstype_arena_kode"),
            ),
        )
    }

    private fun statuserWhereStatement(statuser: List<Avtalestatus>): String =
        statuser
            .ifEmpty { null }
            ?.joinToString(prefix = "(", postfix = ")", separator = " or ") {
                when (it) {
                    Avtalestatus.Aktiv -> "(avslutningsstatus = '${Avslutningsstatus.IKKE_AVSLUTTET}' and :today <= slutt_dato)"
                    Avtalestatus.Avsluttet -> "(avslutningsstatus = '${Avslutningsstatus.AVSLUTTET}' or :today > slutt_dato)"
                    Avtalestatus.Avbrutt -> "avslutningsstatus = '${Avslutningsstatus.AVBRUTT}'"
                }
            }
            ?: "true"

    private fun Row.toAvtaleNotificationDto(): AvtaleNotificationDto {
        val administratorer = arrayOrNull<String?>("administratorer")?.asList()?.filterNotNull() ?: emptyList()
        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")

        return AvtaleNotificationDto(
            id = uuid("id"),
            navn = string("navn"),
            startDato = startDato,
            sluttDato = sluttDato,
            administratorer = administratorer.map { NavIdent(it) },
        )
    }
}
