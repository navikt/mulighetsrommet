package no.nav.mulighetsrommet.api.avtale.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.OpsjonsmodellData
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.database.*
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleQueries(private val session: Session) {

    fun upsert(avtale: AvtaleDbo) = withTransaction(session) {
        @Language("PostgreSQL")
        val query = """
            insert into avtale (
                id,
                navn,
                tiltakstype_id,
                avtalenummer,
                sakarkiv_nummer,
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
                opsjon_custom_opsjonsmodell_navn,
                prismodell
            ) values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :avtalenummer,
                :sakarkiv_nummer,
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
                :opsjonCustomOpsjonsmodellNavn,
                :prismodell::prismodell
            ) on conflict (id) do update set
                navn                        = excluded.navn,
                tiltakstype_id              = excluded.tiltakstype_id,
                avtalenummer                = excluded.avtalenummer,
                sakarkiv_nummer             = excluded.sakarkiv_nummer,
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
                opsjon_custom_opsjonsmodell_navn = excluded.opsjon_custom_opsjonsmodell_navn,
                prismodell                  = excluded.prismodell
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
        val setArrangorUnderenhet = """
             insert into avtale_arrangor_underenhet (avtale_id, arrangor_id)
             values (?::uuid, ?::uuid)
             on conflict (avtale_id, arrangor_id) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUnderenheter = """
             delete from avtale_arrangor_underenhet
             where avtale_id = ?::uuid and not (arrangor_id = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into avtale_arrangor_kontaktperson (avtale_id, arrangor_kontaktperson_id)
            values (?::uuid, ?::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from avtale_arrangor_kontaktperson
            where avtale_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertPersonopplysninger = """
            insert into avtale_personopplysning (avtale_id, personopplysning)
            values (?::uuid, ?::personopplysning)
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

        execute(queryOf(query, avtale.toSqlParameters()))

        batchPreparedStatement(upsertAdministrator, avtale.administratorer.map { listOf(avtale.id, it.value) })
        execute(queryOf(deleteAdministratorer, avtale.id, createArrayOfValue(avtale.administratorer) { it.value }))

        batchPreparedStatement(upsertEnhet, avtale.navEnheter.map { listOf(avtale.id, it.value) })
        execute(queryOf(deleteEnheter, avtale.id, createArrayOfValue(avtale.navEnheter) { it.value }))

        avtale.arrangor?.underenheter?.let { underenheter ->
            batchPreparedStatement(setArrangorUnderenhet, underenheter.map { listOf(avtale.id, it) })
        }
        execute(queryOf(deleteUnderenheter, avtale.id, avtale.arrangor?.underenheter?.let { createUuidArray(it) }))

        avtale.arrangor?.kontaktpersoner?.let { kontaktpersoner ->
            batchPreparedStatement(upsertArrangorKontaktperson, kontaktpersoner.map { listOf(avtale.id, it) })
        }
        execute(
            queryOf(
                deleteArrangorKontaktpersoner,
                avtale.id,
                avtale.arrangor?.kontaktpersoner?.let { createUuidArray(it) },
            ),
        )

        batchPreparedStatement(
            upsertPersonopplysninger,
            avtale.personopplysninger.map { listOf<Any>(avtale.id, it.name) },
        )
        execute(
            queryOf(
                deletePersonopplysninger,
                avtale.id,
                createArrayOfPersonopplysning(avtale.personopplysninger),
            ),
        )

        AmoKategoriseringQueries(this).upsert(avtale)

        execute(queryOf(deleteUtdanningslop, avtale.id))

        avtale.utdanningslop?.let { utdanningslop ->
            val utdanninger = utdanningslop.utdanninger.map {
                mapOf(
                    "avtale_id" to avtale.id,
                    "utdanningsprogram_id" to utdanningslop.utdanningsprogram,
                    "utdanning_id" to it,
                )
            }
            batchPreparedNamedStatement(insertUtdanningslop, utdanninger)
        }
    }

    fun upsertArenaAvtale(avtale: ArenaAvtaleDbo) = withTransaction(session) {
        val arrangorId = single(
            queryOf("select id from arrangor where organisasjonsnummer = ?", avtale.arrangorOrganisasjonsnummer),
        ) { it.uuid("id") }.let { requireNotNull(it) }

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

        execute(queryOf(query, avtale.toSqlParameters(arrangorId)))
    }

    fun get(id: UUID): AvtaleDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from avtale_admin_dto_view
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toAvtaleDto() }
    }

    fun getAll(
        pagination: Pagination = Pagination.all(),
        tiltakstypeIder: List<UUID> = emptyList(),
        search: String? = null,
        statuser: List<AvtaleStatus.Enum> = emptyList(),
        avtaletyper: List<Avtaletype> = emptyList(),
        navRegioner: List<NavEnhetNummer> = emptyList(),
        sortering: String? = null,
        arrangorIds: List<UUID> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        personvernBekreftet: Boolean? = null,
    ): PaginatedResult<AvtaleDto> = with(session) {
        val parameters = mapOf(
            "search" to search?.toFTSPrefixQuery(),
            "search_arrangor" to search?.trim()?.let { "%$it%" },
            "administrator_nav_ident" to administratorNavIdent?.let { """[{ "navIdent": "${it.value}" }]""" },
            "tiltakstype_ids" to tiltakstypeIder.ifEmpty { null }?.let { createUuidArray(it) },
            "arrangor_ids" to arrangorIds.ifEmpty { null }?.let { createUuidArray(it) },
            "nav_enheter" to navRegioner.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "avtaletyper" to avtaletyper.ifEmpty { null }?.let { createArrayOfAvtaletype(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createTextArray(statuser) },
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

        return queryOf(query, parameters + pagination.parameters)
            .mapPaginated { it.toAvtaleDto() }
            .runWithSession(this)
    }

    fun setOpphav(id: UUID, opphav: ArenaMigrering.Opphav) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set opphav = :opphav::opphav
            where id = :id::uuid
        """.trimIndent()

        update(queryOf(query, mapOf("id" to id, "opphav" to opphav.name)))
    }

    fun avbryt(id: UUID, tidspunkt: LocalDateTime, aarsak: AvbruttAarsak): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale set
                avbrutt_tidspunkt = :tidspunkt,
                avbrutt_aarsak = :aarsak
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf("id" to id, "tidspunkt" to tidspunkt, "aarsak" to aarsak.name)

        return update(queryOf(query, params))
    }

    fun delete(id: UUID) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale
            where id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, id))
    }

    fun frikobleKontaktpersonFraAvtale(
        kontaktpersonId: UUID,
        avtaleId: UUID,
    ) = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from avtale_arrangor_kontaktperson
            where arrangor_kontaktperson_id = ?::uuid and avtale_id = ?::uuid
        """.trimIndent()

        execute(queryOf(query, kontaktpersonId, avtaleId))
    }

    fun getAvtaleIdsByAdministrator(navIdent: NavIdent): List<UUID> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select avtale_id
            from avtale_administrator
            where nav_ident = ?
        """.trimIndent()

        return list(queryOf(query, navIdent.value)) { it.uuid("avtale_id") }
    }

    fun getUpdatedAt(id: UUID): LocalDateTime {
        @Language("PostgreSQL")
        val query = """
            select updated_at from avtale where id = ?
        """.trimIndent()

        return session.requireSingle(queryOf(query, id)) { it.localDateTime("updated_at") }
    }

    fun oppdaterSluttdato(avtaleId: UUID, sluttDato: LocalDate) = with(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale
            set slutt_dato = ?
            where id = ?::uuid
            """

        update(queryOf(query, sluttDato, avtaleId))
    }

    private fun AvtaleDbo.toSqlParameters() = mapOf(
        "opphav" to ArenaMigrering.Opphav.TILTAKSADMINISTRASJON.name,
        "id" to id,
        "navn" to navn,
        "tiltakstype_id" to tiltakstypeId,
        "avtalenummer" to avtalenummer,
        "sakarkiv_nummer" to sakarkivNummer?.value,
        "arrangor_hovedenhet_id" to arrangor?.hovedenhet,
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
        "prismodell" to prismodell?.name,
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
        val administratorer = stringOrNull("administratorer_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.Administrator>>(it) }
            ?: emptyList()
        val navEnheter = stringOrNull("nav_enheter_json")
            ?.let { Json.decodeFromString<List<NavEnhetDbo>>(it) }
            ?: emptyList()
        val kontorstruktur = fromNavEnheter(navEnheter)

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

        val arrangor = uuidOrNull("arrangor_hovedenhet_id")?.let {
            val underenheter = stringOrNull("arrangor_underenheter_json")
                ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorUnderenhet>>(it) }
                ?: emptyList()
            val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<ArrangorKontaktperson>>(it) } ?: emptyList()
            AvtaleDto.ArrangorHovedenhet(
                id = it,
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_hovedenhet_organisasjonsnummer")),
                navn = string("arrangor_hovedenhet_navn"),
                slettet = boolean("arrangor_hovedenhet_slettet"),
                underenheter = underenheter,
                kontaktpersoner = arrangorKontaktpersoner,
            )
        }

        return AvtaleDto(
            id = uuid("id"),
            navn = string("navn"),
            avtalenummer = stringOrNull("avtalenummer"),
            sakarkivNummer = stringOrNull("sakarkiv_nummer")?.let { SakarkivNummer(it) },
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
            arrangor = arrangor,
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
            opsjonerRegistrert = opsjonerRegistrert.sortedBy { it.registrertDato },
            amoKategorisering = amoKategorisering,
            utdanningslop = utdanningslop,
            prismodell = stringOrNull("prismodell")?.let { Prismodell.valueOf(it) },
        )
    }
}

fun Session.createArrayOfAvtaletype(
    avtaletypes: List<Avtaletype>,
): Array = createArrayOf("avtaletype", avtaletypes)

fun Session.createArrayOfPersonopplysning(
    personopplysnings: List<Personopplysning>,
): Array = createArrayOf("personopplysning", personopplysnings)
