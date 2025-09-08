package no.nav.mulighetsrommet.api.avtale.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.amo.AmoKategoriseringQueries
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur.Companion.fromNavEnheter
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.database.*
import no.nav.mulighetsrommet.database.datatypes.toDaterange
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
                status,
                opsjon_maks_varighet,
                avtaletype,
                prisbetingelser,
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
                :status::avtale_status,
                :opsjonMaksVarighet,
                :avtaletype::avtaletype,
                :prisbetingelser,
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
                status                      = excluded.status,
                opsjon_maks_varighet        = excluded.opsjon_maks_varighet,
                avtaletype                  = excluded.avtaletype,
                prisbetingelser             = excluded.prisbetingelser,
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

        upsertPrismodell(avtale.id, avtale.prismodell, avtale.prisbetingelser, avtale.satser)
    }

    fun upsertPrismodell(id: UUID, prismodell: Prismodell, prisbetingelser: String?, satser: List<AvtaltSats>) = withTransaction(session) {
        upsertPrismodell(id, prismodell, prisbetingelser, satser)
    }

    private fun Session.upsertPrismodell(
        id: UUID,
        prismodell: Prismodell,
        prisbetingelser: String?,
        satser: List<AvtaltSats>,
    ) {
        @Language("PostgreSQL")
        val query = """
            update avtale set
                prisbetingelser = :prisbetingelser,
                prismodell = :prismodell::prismodell
            where id = :id::uuid
        """.trimIndent()

        execute(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "prismodell" to prismodell.name,
                    "prisbetingelser" to prisbetingelser,
                ),
            ),
        )

        @Language("PostgreSQL")
        val deleteSatser = """
            delete from avtale_sats
            where avtale_id = ?::uuid
        """.trimIndent()
        execute(queryOf(deleteSatser, id))

        @Language("PostgreSQL")
        val insertSats = """
            insert into avtale_sats (avtale_id, gjelder_fra, sats)
            values (:avtale_id::uuid, :gjelder_fra::date, :sats)
        """.trimIndent()

        batchPreparedNamedStatement(
            insertSats,
            satser.map {
                mapOf(
                    "avtale_id" to id,
                    "gjelder_fra" to it.gjelderFra,
                    "sats" to it.sats,
                )
            },
        )
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
                               status,
                               opsjonsmodell,
                               arena_ansvarlig_enhet,
                               avtaletype,
                               prismodell,
                               prisbetingelser,
                               opphav)
            values (:id::uuid,
                    :navn,
                    :tiltakstype_id::uuid,
                    :avtalenummer,
                    :arrangor_hovedenhet_id,
                    :start_dato,
                    :slutt_dato,
                    :status::avtale_status,
                    :opsjonsmodell::opsjonsmodell,
                    :arena_ansvarlig_enhet,
                    :avtaletype::avtaletype,
                    :prismodell::prismodell,
                    :prisbetingelser,
                    :opphav::opphav)
            on conflict (id) do update set navn                     = excluded.navn,
                                           tiltakstype_id           = excluded.tiltakstype_id,
                                           avtalenummer             = excluded.avtalenummer,
                                           arrangor_hovedenhet_id   = excluded.arrangor_hovedenhet_id,
                                           start_dato               = excluded.start_dato,
                                           slutt_dato               = excluded.slutt_dato,
                                           status                   = excluded.status,
                                           opsjonsmodell            = coalesce(avtale.opsjonsmodell, excluded.opsjonsmodell),
                                           arena_ansvarlig_enhet    = excluded.arena_ansvarlig_enhet,
                                           avtaletype               = excluded.avtaletype,
                                           prismodell               = coalesce(avtale.prismodell, excluded.prismodell),
                                           prisbetingelser          = excluded.prisbetingelser,
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
        statuser: List<AvtaleStatus> = emptyList(),
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
            "statuser" to statuser.ifEmpty { null }?.let { createArrayOfAvtaleStatus(statuser) },
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

    fun setStatus(
        id: UUID,
        status: AvtaleStatus,
        tidspunkt: LocalDateTime?,
        aarsakerOgForklaring: AarsakerOgForklaringRequest<AvbruttAarsak>?,
    ): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            update avtale set
                status = :status::avtale_status,
                avbrutt_tidspunkt = :tidspunkt,
                avbrutt_aarsaker = :aarsaker,
                avbrutt_forklaring = :forklaring
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "status" to status.name,
            "tidspunkt" to tidspunkt,
            "aarsaker" to aarsakerOgForklaring?.aarsaker?.let { session.createTextArray(it.map { it.name }) },
            "forklaring" to aarsakerOgForklaring?.forklaring,
        )
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

    fun setSluttDato(avtaleId: UUID, sluttDato: LocalDate) = with(session) {
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
        "status" to status.name,
        "avtaletype" to avtaletype.name,
        "prisbetingelser" to prisbetingelser,
        "beskrivelse" to beskrivelse,
        "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
        "personvern_bekreftet" to personvernBekreftet,
        "opsjonsmodell" to opsjonsmodell.type.name,
        "opsjonMaksVarighet" to opsjonsmodell.opsjonMaksVarighet,
        "opsjonCustomOpsjonsmodellNavn" to opsjonsmodell.customOpsjonsmodellNavn,
        "prismodell" to prismodell.name,
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
            "status" to when (avslutningsstatus) {
                Avslutningsstatus.IKKE_AVSLUTTET -> AvtaleStatus.AKTIV
                Avslutningsstatus.AVSLUTTET -> AvtaleStatus.AVSLUTTET
                Avslutningsstatus.AVLYST, Avslutningsstatus.AVBRUTT -> AvtaleStatus.AVBRUTT
            }.name,
            "opsjonsmodell" to when (avtaletype) {
                Avtaletype.FORHANDSGODKJENT, Avtaletype.OFFENTLIG_OFFENTLIG -> OpsjonsmodellType.VALGFRI_SLUTTDATO
                else -> OpsjonsmodellType.INGEN_OPSJONSMULIGHET
            }.name,
            "arena_ansvarlig_enhet" to arenaAnsvarligEnhet,
            "avtaletype" to avtaletype.name,
            "avbrutt_tidspunkt" to avbruttTidspunkt,
            "avbrutt_aarsaker" to if (avbruttTidspunkt != null) session.createTextArray(listOf("AVBRUTT_I_ARENA")) else null,
            "avbrutt_forklaring" to null,
            "prismodell" to when (avtaletype) {
                Avtaletype.FORHANDSGODKJENT -> Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
                else -> Prismodell.ANNEN_AVTALT_PRIS
            }.name,
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
            ?.let { Json.decodeFromString<List<NavEnhetDto>>(it) }
            ?: emptyList()
        val kontorstruktur = fromNavEnheter(navEnheter)

        val opsjonerRegistrert = stringOrNull("opsjon_logg_json")
            ?.let { Json.decodeFromString<List<AvtaleDto.OpsjonLoggRegistrert>>(it) }
            ?: emptyList()

        val opsjonsmodell = Opsjonsmodell(
            type = OpsjonsmodellType.valueOf(string("opsjonsmodell")),
            opsjonMaksVarighet = localDateOrNull("opsjon_maks_varighet"),
            customOpsjonsmodellNavn = stringOrNull("opsjon_custom_opsjonsmodell_navn"),
        )
        val amoKategorisering = stringOrNull("amo_kategorisering_json")
            ?.let { JsonIgnoreUnknownKeys.decodeFromString<AmoKategorisering>(it) }

        val utdanningslop = stringOrNull("utdanningslop_json")
            ?.let { Json.decodeFromString<UtdanningslopDto>(it) }

        val arrangor = uuidOrNull("arrangor_hovedenhet_id")?.let { id ->
            val underenheter = stringOrNull("arrangor_underenheter_json")
                ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorUnderenhet>>(it) }
                ?: emptyList()
            val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<AvtaleDto.ArrangorKontaktperson>>(it) } ?: emptyList()
            AvtaleDto.ArrangorHovedenhet(
                id = id,
                organisasjonsnummer = Organisasjonsnummer(string("arrangor_hovedenhet_organisasjonsnummer")),
                navn = string("arrangor_hovedenhet_navn"),
                slettet = boolean("arrangor_hovedenhet_slettet"),
                underenheter = underenheter,
                kontaktpersoner = arrangorKontaktpersoner,
            )
        }

        val satser = stringOrNull("satser_json")
            ?.let { Json.decodeFromString<List<AvtaltSats>>(it) }
            ?: emptyList()

        val prismodell = when (Prismodell.valueOf(string("prismodell"))) {
            Prismodell.ANNEN_AVTALT_PRIS -> AvtaleDto.PrismodellDto.AnnenAvtaltPris(
                prisbetingelser = stringOrNull("prisbetingelser"),
            )

            Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk

            Prismodell.AVTALT_PRIS_PER_MANEDSVERK -> AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk(
                prisbetingelser = stringOrNull("prisbetingelser"),
                satser = satser.toDto()
            )

            Prismodell.AVTALT_PRIS_PER_UKESVERK -> AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk(
                prisbetingelser = stringOrNull("prisbetingelser"),
                satser = satser.toDto(),
            )

            Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER -> AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker(
                prisbetingelser = stringOrNull("prisbetingelser"),
                satser = satser.toDto()
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
            status = AvtaleStatusDto.fromString(
                string("status"),
                localDateTimeOrNull("avbrutt_tidspunkt"),
                arrayOrNull<String>("avbrutt_aarsaker")?.map { AvbruttAarsak.valueOf(it) } ?: emptyList(),
                stringOrNull("avbrutt_forklaring"),
            ),
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
            opsjonsmodell = opsjonsmodell,
            opsjonerRegistrert = opsjonerRegistrert.sortedBy { it.registrertDato },
            amoKategorisering = amoKategorisering,
            utdanningslop = utdanningslop,
            prismodell = prismodell,
        )
    }
}

fun Session.createArrayOfAvtaleStatus(
    avtaletypes: List<AvtaleStatus>,
): Array = createArrayOf("avtale_status", avtaletypes)

fun Session.createArrayOfAvtaletype(
    avtaletypes: List<Avtaletype>,
): Array = createArrayOf("avtaletype", avtaletypes)

fun Session.createArrayOfPersonopplysning(
    personopplysnings: List<Personopplysning>,
): Array = createArrayOf("personopplysning", personopplysnings)
