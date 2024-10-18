package no.nav.mulighetsrommet.api.repositories

import arrow.core.Either
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.responses.StatusResponseError
import no.nav.mulighetsrommet.api.routes.v1.Opsjonsmodell
import no.nav.mulighetsrommet.api.routes.v1.OpsjonsmodellData
import no.nav.mulighetsrommet.api.utils.DBUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(avtale: AvtaleDbo) = db.transaction { upsert(avtale, it) }

    fun upsert(avtale: AvtaleDbo, tx: Session) {
        logger.info("Lagrer avtale id=${avtale.id}")

        @Language("PostgreSQL")
        val query = """
            insert into avtale (
                id,
                navn,
                tiltakstype_id,
                avtalenummer,
                websaknummer,
                arrangor_hovedenhet_id,
                start_dato,
                slutt_dato,
                opsjon_maks_varighet,
                avtaletype,
                prisbetingelser,
                antall_plasser,
                opphav,
                beskrivelse,
                faneinnhold,
                personvern_bekreftet,
                opsjonsmodell,
                opsjon_custom_opsjonsmodell_navn
            ) values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :avtalenummer,
                :websaknummer,
                :arrangor_hovedenhet_id,
                :start_dato,
                :slutt_dato,
                :opsjonMaksVarighet,
                :avtaletype::avtaletype,
                :prisbetingelser,
                :antall_plasser,
                :opphav::opphav,
                :beskrivelse,
                :faneinnhold::jsonb,
                :personvern_bekreftet,
                :opsjonsmodell::opsjonsmodell,
                :opsjonCustomOpsjonsmodellNavn
            ) on conflict (id) do update set
                navn                        = excluded.navn,
                tiltakstype_id              = excluded.tiltakstype_id,
                avtalenummer                = excluded.avtalenummer,
                websaknummer                = excluded.websaknummer,
                arrangor_hovedenhet_id      = excluded.arrangor_hovedenhet_id,
                start_dato                  = excluded.start_dato,
                slutt_dato                  = excluded.slutt_dato,
                opsjon_maks_varighet        = excluded.opsjon_maks_varighet,
                avtaletype                  = excluded.avtaletype,
                prisbetingelser             = excluded.prisbetingelser,
                antall_plasser              = excluded.antall_plasser,
                opphav                      = coalesce(avtale.opphav, excluded.opphav),
                beskrivelse                 = excluded.beskrivelse,
                faneinnhold                 = excluded.faneinnhold,
                personvern_bekreftet        = excluded.personvern_bekreftet,
                opsjonsmodell               = excluded.opsjonsmodell,
                opsjon_custom_opsjonsmodell_navn = excluded.opsjon_custom_opsjonsmodell_navn
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

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into avtale_arrangor_kontaktperson (
                arrangor_kontaktperson_id,
                avtale_id
            )
            values (:arrangor_kontaktperson_id::uuid, :avtale_id::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from avtale_arrangor_kontaktperson
            where avtale_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertPersonopplysninger = """
            insert into avtale_personopplysning (
                personopplysning,
                avtale_id
            )
            values (:personopplysning::personopplysning, :avtale_id::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deletePersonopplysninger = """
            delete from avtale_personopplysning
            where avtale_id = ?::uuid and not (personopplysning = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUtdanningslop = """
            delete from avtale_utdanningsprogram
            where avtale_id = ?::uuid
        """.trimIndent()

        @Language("PostgreSQL")
        val insertUtdanningslop = """
            insert into avtale_utdanningsprogram(
                avtale_id,
                utdanning_id,
                utdanningsprogram_id
            )
            values(:avtale_id::uuid, :utdanning_id::uuid, :utdanningsprogram_id::uuid)
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

        avtale.arrangorKontaktpersoner.forEach { person ->
            tx.run(
                queryOf(
                    upsertArrangorKontaktperson,
                    mapOf(
                        "avtale_id" to avtale.id,
                        "arrangor_kontaktperson_id" to person,
                    ),
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deleteArrangorKontaktpersoner,
                avtale.id,
                db.createUuidArray(avtale.arrangorKontaktpersoner),
            ).asExecute,
        )

        avtale.personopplysninger.forEach { p ->
            tx.run(
                queryOf(
                    upsertPersonopplysninger,
                    mapOf(
                        "avtale_id" to avtale.id,
                        "personopplysning" to p.name,
                    ),
                ).asExecute,
            )
        }

        tx.run(
            queryOf(
                deletePersonopplysninger,
                avtale.id,
                db.createArrayOf("personopplysning", avtale.personopplysninger),
            ).asExecute,
        )

        AmoKategoriseringRepository.upsert(avtale, tx)

        tx.run(queryOf(deleteUtdanningslop, avtale.id).asExecute)
        avtale.utdanningslop?.let { utdanningslop ->
            utdanningslop.utdanninger.forEach {
                tx.run(
                    queryOf(
                        insertUtdanningslop,
                        mapOf(
                            "avtale_id" to avtale.id,
                            "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                            "utdanning_id" to it,
                        ),
                    ).asExecute,
                )
            }
        }
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
                               avbrutt_tidspunkt,
                               avbrutt_aarsak,
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
                    :avbrutt_tidspunkt,
                    :avbrutt_aarsak,
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
                                           avbrutt_tidspunkt        = excluded.avbrutt_tidspunkt,
                                           avbrutt_aarsak           = excluded.avbrutt_aarsak,
                                           prisbetingelser          = excluded.prisbetingelser,
                                           antall_plasser           = excluded.antall_plasser,
                                           opphav                   = coalesce(avtale.opphav, excluded.opphav)
        """.trimIndent()

        queryOf(query, avtale.toSqlParameters(arrangorId)).asExecute.let { tx.run(it) }
    }

    fun get(id: UUID): AvtaleDto? = db.transaction { get(id, it) }

    fun get(id: UUID, tx: Session): AvtaleDto? {
        @Language("PostgreSQL")
        val query = """
            select *
            from avtale_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return tx.run(
            queryOf(query, id)
                .map { it.toAvtaleDto() }
                .asSingle,
        )
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        tiltakstypeIder: List<UUID> = emptyList(),
        search: String? = null,
        statuser: List<AvtaleStatus.Enum> = emptyList(),
        avtaletyper: List<Avtaletype> = emptyList(),
        navRegioner: List<String> = emptyList(),
        sortering: String? = null,
        arrangorIds: List<UUID> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        personvernBekreftet: Boolean? = null,
    ): PaginatedResult<AvtaleDto> {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { db.createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { db.createUuidArray(it) },
            "nav_enheter" to navRegioner.ifEmpty { null }?.let { db.createTextArray(it) },
            "avtaletyper" to avtaletyper.ifEmpty { null }?.let { db.createArrayOf("avtaletype", it) },
            "statuser" to statuser.ifEmpty { null }?.let { db.createArrayOf("text", statuser) },
            "personvern_bekreftet" to personvernBekreftet,
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
            select *, count(*) over() as total_count
            from avtale_admin_dto_view
            where (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any (:tiltakstype_ids))
              and (:search::text is null or (fts @@ to_tsquery('norwegian', :search) or arrangor_hovedenhet_navn ilike :search_arrangor))
              and (:nav_enheter::text[] is null or (
                   exists(select true
                          from jsonb_array_elements(nav_enheter_json) as nav_enhet
                          where nav_enhet ->> 'enhetsnummer' = any (:nav_enheter)) or
                   arena_nav_enhet_enhetsnummer = any (:nav_enheter) or
                   arena_nav_enhet_enhetsnummer in (select enhetsnummer
                                                    from nav_enhet
                                                    where overordnet_enhet = any (:nav_enheter))))
              and (:arrangor_ids::text[] is null or arrangor_hovedenhet_id = any (:arrangor_ids))
              and (:administrator_nav_ident::text is null or administratorer_json @> :administrator_nav_ident::jsonb)
              and (:avtaletyper::avtaletype[] is null or avtaletype = any (:avtaletyper))
              and (:statuser::text[] is null or status = any(:statuser))
              and (:personvern_bekreftet::boolean is null or personvern_bekreftet = :personvern_bekreftet::boolean)
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        return db.useSession { session ->
            queryOf(query, parameters + pagination.parameters)
                .mapPaginated { it.toAvtaleDto() }
                .runWithSession(session)
        }
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
            where (:currentDate::timestamp + interval '8' month) = a.slutt_dato
               or (:currentDate::timestamp + interval '6' month) = a.slutt_dato
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

    fun avbryt(id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak): Int =
        db.transaction { avbryt(it, id, tidspunkt, aarsak) }

    fun avbryt(tx: Session, id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak): Int {
        @Language("PostgreSQL")
        val query = """
            update avtale set
                avbrutt_tidspunkt = :tidspunkt,
                avbrutt_aarsak = :aarsak
            where id = :id::uuid
        """.trimIndent()

        return tx.run(queryOf(query, mapOf("id" to id, "tidspunkt" to tidspunkt, "aarsak" to aarsak.name)).asUpdate)
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
        "websaknummer" to websaknummer?.value,
        "arrangor_hovedenhet_id" to arrangorId,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
        "opsjonMaksVarighet" to opsjonMaksVarighet,
        "avtaletype" to avtaletype.name,
        "prisbetingelser" to prisbetingelser,
        "antall_plasser" to antallPlasser,
        "beskrivelse" to beskrivelse,
        "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
        "personvern_bekreftet" to personvernBekreftet,
        "opsjonsmodell" to opsjonsmodell?.name,
        "opsjonCustomOpsjonsmodellNavn" to customOpsjonsmodellNavn,
    )

    private fun ArenaAvtaleDbo.toSqlParameters(arrangorId: UUID): Map<String, Any?> {
        val avbruttTidspunkt = when (avslutningsstatus) {
            Avslutningsstatus.AVLYST -> startDato.atStartOfDay().minusDays(1)
            Avslutningsstatus.AVBRUTT -> startDato.atStartOfDay()
            Avslutningsstatus.AVSLUTTET -> null
            Avslutningsstatus.IKKE_AVSLUTTET -> null
        }
        return mapOf(
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
            "avbrutt_tidspunkt" to avbruttTidspunkt,
            "avbrutt_aarsak" to if (avbruttTidspunkt != null) "AVBRUTT_I_ARENA" else null,
            "prisbetingelser" to prisbetingelser,
        )
    }

    private fun Row.toAvtaleDto(): AvtaleDto {
        val startDato = localDate("start_dato")
        val sluttDato = localDateOrNull("slutt_dato")
        val personopplysninger = stringOrNull("personopplysninger_json")
            ?.let { Json.decodeFromString<List<Personopplysning>>(it) }
            ?: emptyList()
        val underenheter = stringOrNull("arrangor_underenheter_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorUnderenhet>>(it) }
            ?: emptyList()
        val administratorer = stringOrNull("administratorer_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.Administrator>>(it) }
            ?: emptyList()
        val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<ArrangorKontaktperson>>(it) }
            ?: emptyList()
        val navEnheter = stringOrNull("nav_enheter_json")
            ?.let { Json.decodeFromString<List<NavEnhetDbo>>(it) }
            ?: emptyList()
        val kontorstruktur = navEnheter
            .filter { it.type == Norg2Type.FYLKE }
            .map { region ->
                val kontorer = navEnheter
                    .filter { enhet -> enhet.type != Norg2Type.FYLKE && enhet.overordnetEnhet == region.enhetsnummer }
                    .sortedBy { enhet -> enhet.navn }
                Kontorstruktur(region = region, kontorer = kontorer)
            }

        val opsjonerRegistrert = stringOrNull("opsjon_logg_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.OpsjonLoggRegistrert>>(it) }
            ?: emptyList()

        val avbruttTidspunkt = localDateTimeOrNull("avbrutt_tidspunkt")
        val avbruttAarsak = stringOrNull("avbrutt_aarsak")?.let { AvbruttAarsak.fromString(it) }

        val opsjonsmodellData = OpsjonsmodellData(
            opsjonMaksVarighet = localDateOrNull("opsjon_maks_varighet"),
            opsjonsmodell = stringOrNull("opsjonsmodell")?.let { Opsjonsmodell.valueOf(it) },
            customOpsjonsmodellNavn = stringOrNull("opsjon_custom_opsjonsmodell_navn"),
        )
        val amoKategorisering = stringOrNull("amo_kategorisering_json")
            ?.let { JsonIgnoreUnknownKeys.decodeFromString<AmoKategorisering>(it) }

        val utdanningslop = stringOrNull("utdanningslop_json")
            ?.let { Json.decodeFromString<UtdanningslopDto>(it) }

        return AvtaleDto(
            id = uuid("id"),
            navn = string("navn"),
            avtalenummer = stringOrNull("avtalenummer"),
            websaknummer = stringOrNull("websaknummer")?.let { Websaknummer(it) },
            startDato = startDato,
            sluttDato = sluttDato,
            opphav = ArenaMigrering.Opphav.valueOf(string("opphav")),
            avtaletype = Avtaletype.valueOf(string("avtaletype")),
            status = AvtaleStatus.fromString(string("status"), avbruttTidspunkt, avbruttAarsak),
            prisbetingelser = stringOrNull("prisbetingelser"),
            antallPlasser = intOrNull("antall_plasser"),
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            administratorer = administratorer,
            kontorstruktur = kontorstruktur,
            arrangor = AvtaleDto.ArrangorHovedenhet(
                id = uuid("arrangor_hovedenhet_id"),
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_hovedenhet_organisasjonsnummer")),
                navn = string("arrangor_hovedenhet_navn"),
                slettet = boolean("arrangor_hovedenhet_slettet"),
                underenheter = underenheter,
                kontaktpersoner = arrangorKontaktpersoner,
            ),
            arenaAnsvarligEnhet = stringOrNull("arena_nav_enhet_enhetsnummer")?.let {
                ArenaNavEnhet(
                    navn = stringOrNull("arena_nav_enhet_navn"),
                    enhetsnummer = it,
                )
            },
            tiltakstype = AvtaleDto.Tiltakstype(
                id = uuid("tiltakstype_id"),
                navn = string("tiltakstype_navn"),
                tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
            ),
            personopplysninger = personopplysninger,
            personvernBekreftet = boolean("personvern_bekreftet"),
            opsjonsmodellData = opsjonsmodellData,
            opsjonerRegistrert = opsjonerRegistrert.sortedBy { it.aktivertDato },
            amoKategorisering = amoKategorisering,
            utdanningslop = utdanningslop,
        )
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
            administratorer = administratorer.map { NavIdent(it) },
        )
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
        tx: Session,
    ): Either<StatusResponseError, String> {
        @Language("PostgreSQL")
        val query = """
            delete from avtale_arrangor_kontaktperson where arrangor_kontaktperson_id = ?::uuid and avtale_id = ?::uuid
        """.trimIndent()

        queryOf(query, kontaktpersonId, avtaleId)
            .asUpdate
            .let { tx.run(it) }

        return Either.Right(kontaktpersonId.toString())
    }

    fun getAvtaleIdsByAdministrator(navIdent: NavIdent): List<UUID> {
        @Language("PostgreSQL")
        val query = """
            select avtale_id from avtale_administrator where nav_ident = ?
        """.trimIndent()

        return queryOf(query, navIdent.value)
            .map {
                it.uuid("avtale_id")
            }
            .asList
            .let { db.run(it) }
    }

    fun oppdaterSluttdato(avtaleId: UUID, nySluttdato: LocalDate, tx: Session? = null) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set slutt_dato = :nySluttdato
            where id = :avtaleId::uuid
            """

        queryOf(
            query,
            mapOf(
                "nySluttdato" to nySluttdato,
                "avtaleId" to avtaleId,
            ),
        ).asUpdate.let {
            tx?.run(it) ?: db.run(it)
        }
    }
}
