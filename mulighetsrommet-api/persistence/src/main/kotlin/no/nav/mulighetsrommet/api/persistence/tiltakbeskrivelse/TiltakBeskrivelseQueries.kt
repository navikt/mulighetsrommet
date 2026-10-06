package no.nav.mulighetsrommet.api.persistence.tiltakbeskrivelse

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseDto
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseKompaktDto
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseQueryHandler
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelse
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelseRepository
import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.utils.parameters
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.UUID

class TiltakBeskrivelseQueries(private val session: Session) : TiltakBeskrivelseRepository, TiltakBeskrivelseQueryHandler {
    override fun save(tiltakBeskrivelse: TiltakBeskrivelse): Unit = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltak_beskrivelse (
                id,
                navn,
                tiltakstype_id,
                sted_for_gjennomforing,
                arrangor_id,
                faneinnhold,
                beskrivelse,
                tiltaksnummer,
                sanity_id,
                publisert
            )
            values (
                :id::uuid,
                :navn,
                :tiltakstype_id::uuid,
                :sted_for_gjennomforing,
                :arrangor_id::uuid,
                :faneinnhold::jsonb,
                :beskrivelse,
                :tiltaksnummer,
                :sanity_id::uuid,
                :publisert
            )
            on conflict (id) do update set
                navn                   = excluded.navn,
                tiltakstype_id         = excluded.tiltakstype_id,
                sted_for_gjennomforing = excluded.sted_for_gjennomforing,
                arrangor_id            = excluded.arrangor_id,
                faneinnhold            = excluded.faneinnhold,
                beskrivelse            = excluded.beskrivelse,
                sanity_id              = coalesce(excluded.sanity_id, tiltak_beskrivelse.sanity_id),
                tiltaksnummer          = excluded.tiltaksnummer,
                publisert              = excluded.publisert,
                updated_at             = now()
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertAdministrator = """
            insert into tiltak_beskrivelse_administrator (tiltak_beskrivelse_id, nav_ident)
            values (:id::uuid, :nav_ident)
            on conflict (tiltak_beskrivelse_id, nav_ident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAdministratorer = """
            delete from tiltak_beskrivelse_administrator
            where tiltak_beskrivelse_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
            insert into tiltak_beskrivelse_nav_enhet (tiltak_beskrivelse_id, enhetsnummer)
            values (:id::uuid, :enhetsnummer)
            on conflict (tiltak_beskrivelse_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
            delete from tiltak_beskrivelse_nav_enhet
            where tiltak_beskrivelse_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertKontaktperson = """
            insert into tiltak_beskrivelse_kontaktperson (tiltak_beskrivelse_id, kontaktperson_nav_ident, beskrivelse)
            values (:id::uuid, :nav_ident, :beskrivelse)
            on conflict (tiltak_beskrivelse_id, kontaktperson_nav_ident) do update set
                beskrivelse = :beskrivelse
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from tiltak_beskrivelse_kontaktperson
            where tiltak_beskrivelse_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into tiltak_beskrivelse_arrangor_kontaktperson (tiltak_beskrivelse_id, arrangor_kontaktperson_id)
            values (:tiltak_beskrivelse_id::uuid, :arrangor_kontaktperson_id::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from tiltak_beskrivelse_arrangor_kontaktperson
            where tiltak_beskrivelse_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        execute(
            queryOf(
                query,
                mapOf(
                    "id" to tiltakBeskrivelse.id,
                    "navn" to tiltakBeskrivelse.navn,
                    "tiltakstype_id" to tiltakBeskrivelse.tiltakstypeId,
                    "sted_for_gjennomforing" to tiltakBeskrivelse.stedForGjennomforing,
                    "arrangor_id" to tiltakBeskrivelse.arrangorId,
                    "faneinnhold" to tiltakBeskrivelse.faneinnhold?.let { Json.encodeToString(it) },
                    "beskrivelse" to tiltakBeskrivelse.beskrivelse,
                    "tiltaksnummer" to tiltakBeskrivelse.tiltaksnummer,
                    "sanity_id" to tiltakBeskrivelse.sanityId,
                    "publisert" to tiltakBeskrivelse.publisert,
                ),
            ),
        )
        batchPreparedNamedStatement(
            upsertAdministrator,
            tiltakBeskrivelse.administratorer.map { mapOf("id" to tiltakBeskrivelse.id, "nav_ident" to it.value) },
        )
        execute(
            queryOf(
                deleteAdministratorer,
                tiltakBeskrivelse.id,
                createArrayOfValue(tiltakBeskrivelse.administratorer) { it.value },
            ),
        )

        batchPreparedNamedStatement(
            upsertEnhet,
            tiltakBeskrivelse.navEnheter.map { mapOf("id" to tiltakBeskrivelse.id, "enhetsnummer" to it.value) },
        )
        execute(
            queryOf(
                deleteEnheter,
                tiltakBeskrivelse.id,
                createArrayOfValue(tiltakBeskrivelse.navEnheter) { it.value },
            ),
        )

        batchPreparedNamedStatement(
            upsertKontaktperson,
            tiltakBeskrivelse.kontaktpersoner.map { mapOf("id" to tiltakBeskrivelse.id, "nav_ident" to it.navIdent.value, "beskrivelse" to it.beskrivelse) },
        )
        execute(
            queryOf(
                deleteKontaktpersoner,
                tiltakBeskrivelse.id,
                createArrayOfValue(tiltakBeskrivelse.kontaktpersoner) { it.navIdent.value },
            ),
        )

        batchPreparedNamedStatement(
            upsertArrangorKontaktperson,
            tiltakBeskrivelse.arrangorKontaktpersoner.map { mapOf("tiltak_beskrivelse_id" to tiltakBeskrivelse.id, "arrangor_kontaktperson_id" to it) },
        )
        execute(
            queryOf(
                deleteArrangorKontaktpersoner,
                tiltakBeskrivelse.id,
                createUuidArray(tiltakBeskrivelse.arrangorKontaktpersoner),
            ),
        )
    }

    override fun upsertFromArena(tiltakBeskrivelse: TiltakBeskrivelse): Unit = with(session) {
        @Language("PostgreSQL")
        val query = """
            with resolved as (
                select coalesce(
                    (select id from tiltak_beskrivelse where sanity_id = :sanity_id::uuid),
                    :id::uuid
                ) as id
            )
            insert into tiltak_beskrivelse (id, sanity_id, navn, tiltaksnummer, tiltakstype_id, arrangor_id, sted_for_gjennomforing, faneinnhold, beskrivelse, publisert)
            select r.id, :sanity_id::uuid, :navn, :tiltaksnummer, :tiltakstype_id::uuid, :arrangor_id::uuid, null, null, null, false
            from resolved r
            on conflict (id) do update set
                sanity_id      = excluded.sanity_id,
                navn           = excluded.navn,
                tiltaksnummer  = excluded.tiltaksnummer,
                tiltakstype_id = excluded.tiltakstype_id,
                arrangor_id    = excluded.arrangor_id,
                updated_at     = now()
        """.trimIndent()

        execute(
            queryOf(
                query,
                mapOf(
                    "id" to tiltakBeskrivelse.id,
                    "sanity_id" to tiltakBeskrivelse.sanityId,
                    "navn" to tiltakBeskrivelse.navn,
                    "tiltaksnummer" to tiltakBeskrivelse.tiltaksnummer,
                    "tiltakstype_id" to tiltakBeskrivelse.tiltakstypeId,
                    "arrangor_id" to tiltakBeskrivelse.arrangorId,
                ),
            ),
        )
    }

    override fun getAllKompaktDto(
        pagination: Pagination,
        navEnheter: List<NavEnhetNummer>,
        tiltakstyper: List<Tiltakskode>,
        publisert: Boolean?,
        sortering: String?,
        administratorNavIdent: NavIdent?,
    ): PaginatedResult<TiltakBeskrivelseKompaktDto> = with(session) {
        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "tiltakstype-ascending" -> "tiltakstype_navn asc"
            "tiltakstype-descending" -> "tiltakstype_navn desc"
            "arrangor-ascending" -> "arrangor_navn asc"
            "arrangor-descending" -> "arrangor_navn desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
        select *, count(*) over () as total_count
        from view_tiltak_beskrivelse
        where (:nav_enheter::text[] is null or id in (
            select tiltak_beskrivelse_id from tiltak_beskrivelse_nav_enhet
            where enhetsnummer = any (:nav_enheter)
               or enhetsnummer in (
                   select overordnet_enhet from nav_enhet
                   where enhetsnummer = any (:nav_enheter)
                     and overordnet_enhet is not null
               )
        ))
        and (:tiltakskoder::text[] is null or tiltakstype_tiltakskode = any (:tiltakskoder))
        and (:publisert::boolean is null or publisert = :publisert)
        and (:administrator_nav_ident::text is null or id in (
            select tiltak_beskrivelse_id from tiltak_beskrivelse_administrator
            where nav_ident = :administrator_nav_ident
        ))
        order by $order
        limit :limit
        offset :offset
        """.trimIndent()

        val params = mapOf(
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakskoder" to tiltakstyper.ifEmpty { null }?.let { createArrayOfValue(it) { it.name } },
            "publisert" to publisert,
            "administrator_nav_ident" to administratorNavIdent?.value,
        )

        return queryOf(query, params + pagination.parameters)
            .mapPaginated { toTiltakBeskrivelseKompakt(it) }
            .runWithSession(this)
    }

    override fun setPublisert(id: UUID, publisert: Boolean) {
        @Language("PostgreSQL")
        val query = """
            update tiltak_beskrivelse
                set publisert  = ?, updated_at = now()
            where id = ?::uuid
        """.trimIndent()
        session.execute(queryOf(query, publisert, id))
    }

    override fun get(id: UUID): TiltakBeskrivelse? {
        @Language("PostgreSQL")
        val query = """
        select td.id,
               td.navn,
               td.sanity_id,
               td.tiltaksnummer,
               td.tiltakstype_id,
               td.sted_for_gjennomforing,
               td.arrangor_id,
               td.faneinnhold,
               td.beskrivelse,
               td.publisert,
               (select jsonb_agg(adm.nav_ident)
                from tiltak_beskrivelse_administrator adm
                where adm.tiltak_beskrivelse_id = td.id) as administratorer_json,
               (select jsonb_agg(enhet.enhetsnummer)
                from tiltak_beskrivelse_nav_enhet enhet
                where enhet.tiltak_beskrivelse_id = td.id) as nav_enheter_json,
               (select jsonb_agg(jsonb_build_object(
                       'navIdent', kp.kontaktperson_nav_ident,
                       'beskrivelse', kp.beskrivelse
               ))
                from tiltak_beskrivelse_kontaktperson kp
                where kp.tiltak_beskrivelse_id = td.id) as kontaktpersoner_json,
               (select jsonb_agg(akp.arrangor_kontaktperson_id)
                from tiltak_beskrivelse_arrangor_kontaktperson akp
                where akp.tiltak_beskrivelse_id = td.id) as arrangor_kontaktpersoner_json
        from tiltak_beskrivelse td
        where td.id = :id::uuid or td.sanity_id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("id" to id)), ::toTiltakBeskrivelse)
    }

    private fun toTiltakBeskrivelse(row: Row): TiltakBeskrivelse = TiltakBeskrivelse(
        id = row.uuid("id"),
        navn = row.string("navn"),
        sanityId = row.uuidOrNull("sanity_id"),
        tiltaksnummer = row.stringOrNull("tiltaksnummer"),
        tiltakstypeId = row.uuid("tiltakstype_id"),
        stedForGjennomforing = row.stringOrNull("sted_for_gjennomforing"),
        arrangorId = row.uuidOrNull("arrangor_id"),
        faneinnhold = row.stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        beskrivelse = row.stringOrNull("beskrivelse"),
        publisert = row.boolean("publisert"),
        administratorer = row.stringOrNull("administratorer_json")
            ?.let { Json.decodeFromString<List<NavIdent>>(it) }
            ?: emptyList(),
        navEnheter = row.stringOrNull("nav_enheter_json")
            ?.let { Json.decodeFromString<List<NavEnhetNummer>>(it) }
            ?: emptyList(),
        kontaktpersoner = row.stringOrNull("kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<TiltakBeskrivelse.Kontaktperson>>(it) }
            ?: emptyList(),
        arrangorKontaktpersoner = row.stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<String>>(it).map(UUID::fromString) }
            ?: emptyList(),
    )

    override fun delete(id: UUID) {
        session.execute(queryOf("delete from tiltak_beskrivelse where id = ?::uuid or sanity_id = ?::uuid", id, id))
    }

    override fun getTiltakBeskrivelseDto(id: UUID): TiltakBeskrivelseDto? {
        @Language("PostgreSQL")
        val query = "select * from view_tiltak_beskrivelse where id = :id::uuid"
        return session.single(queryOf(query, mapOf("id" to id)), ::toTiltakBeskrivelseDto)
    }

    private fun toTiltakBeskrivelseDto(row: Row): TiltakBeskrivelseDto {
        val tiltakstypeId = row.uuid("tiltakstype_id")
        val arrangorId = row.uuidOrNull("arrangor_id")

        return TiltakBeskrivelseDto(
            id = row.uuid("id"),
            navn = row.string("navn"),
            sanityId = row.uuidOrNull("sanity_id"),
            tiltaksnummer = row.stringOrNull("tiltaksnummer"),
            tiltakstype = tiltakstypeId.let {
                TiltakBeskrivelseDto.Tiltakstype(
                    id = it,
                    navn = row.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(row.string("tiltakstype_tiltakskode")),
                )
            },
            stedForGjennomforing = row.stringOrNull("sted_for_gjennomforing"),
            arrangor = arrangorId?.let {
                TiltakBeskrivelseDto.Arrangor(
                    id = it,
                    navn = row.string("arrangor_navn"),
                    organisasjonsnummer = row.string("arrangor_organisasjonsnummer"),
                )
            },
            administratorer = row.stringOrNull("administratorer_json")
                ?.let { Json.decodeFromString<List<TiltakBeskrivelseDto.Administrator>>(it) }
                ?: emptyList(),
            arrangorKontaktpersoner = row.stringOrNull("arrangor_kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<TiltakBeskrivelseDto.ArrangorKontaktperson>>(it) }
                ?: emptyList(),
            veilederinfo = TiltakBeskrivelseDto.Veilederinfo(
                publisert = row.boolean("publisert"),
                beskrivelse = row.stringOrNull("beskrivelse"),
                faneinnhold = row.stringOrNull("faneinnhold")?.let { Json.Default.decodeFromString(it) },
                kontorstruktur = row.stringOrNull("nav_enheter_json")
                    ?.let { Kontorstruktur.fromNavEnheter(Json.decodeFromString<List<NavEnhetDto>>(it)) }
                    ?: emptyList(),
                kontaktpersoner = row.stringOrNull("kontaktpersoner_json")
                    ?.let { Json.decodeFromString<List<TiltakBeskrivelseDto.Kontaktperson>>(it) }
                    ?: emptyList(),
            ),
            publisert = row.boolean("publisert"),
        )
    }

    private fun toTiltakBeskrivelseKompakt(row: Row): TiltakBeskrivelseKompaktDto {
        val tiltakstypeId = row.uuid("tiltakstype_id")
        val arrangorId = row.uuidOrNull("arrangor_id")

        return TiltakBeskrivelseKompaktDto(
            id = row.uuid("id"),
            navn = row.string("navn"),
            tiltaksnummer = row.stringOrNull("tiltaksnummer"),
            tiltakstype = tiltakstypeId.let {
                TiltakBeskrivelseKompaktDto.Tiltakstype(
                    id = it,
                    navn = row.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(row.string("tiltakstype_tiltakskode")),
                )
            },
            arrangor = arrangorId?.let {
                TiltakBeskrivelseKompaktDto.Arrangor(
                    id = it,
                    navn = row.string("arrangor_navn"),
                    organisasjonsnummer = row.string("arrangor_organisasjonsnummer"),
                )
            },
            publisert = row.boolean("publisert"),
            kontorstruktur = row.stringOrNull("nav_enheter_json")
                ?.let { Kontorstruktur.fromNavEnheter(Json.decodeFromString<List<NavEnhetDto>>(it)) }
                ?: emptyList(),
        )
    }
}
