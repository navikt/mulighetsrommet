package no.nav.mulighetsrommet.api.persistence.tiltakdokument

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentDto
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentKompaktDto
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentQueryHandler
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokumentRepository
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.UUID

class TiltakDokumentQueries(private val session: Session) : TiltakDokumentRepository, TiltakDokumentQueryHandler {
    override fun save(tiltakDokument: TiltakDokument): Unit = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltak_dokument (
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
                sanity_id              = excluded.sanity_id,
                tiltaksnummer          = excluded.tiltaksnummer,
                publisert              = excluded.publisert,
                updated_at             = now()
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertAdministrator = """
            insert into tiltak_dokument_administrator (tiltak_dokument_id, nav_ident)
            values (:id::uuid, :nav_ident)
            on conflict (tiltak_dokument_id, nav_ident) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteAdministratorer = """
            delete from tiltak_dokument_administrator
            where tiltak_dokument_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertEnhet = """
            insert into tiltak_dokument_nav_enhet (tiltak_dokument_id, enhetsnummer)
            values (:id::uuid, :enhetsnummer)
            on conflict (tiltak_dokument_id, enhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteEnheter = """
            delete from tiltak_dokument_nav_enhet
            where tiltak_dokument_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertKontaktperson = """
            insert into tiltak_dokument_kontaktperson (tiltak_dokument_id, kontaktperson_nav_ident, beskrivelse)
            values (:id::uuid, :nav_ident, :beskrivelse)
            on conflict (tiltak_dokument_id, kontaktperson_nav_ident) do update set
                beskrivelse = :beskrivelse
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from tiltak_dokument_kontaktperson
            where tiltak_dokument_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into tiltak_dokument_arrangor_kontaktperson (tiltak_dokument_id, arrangor_kontaktperson_id)
            values (:tiltak_dokument_id::uuid, :arrangor_kontaktperson_id::uuid)
            on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from tiltak_dokument_arrangor_kontaktperson
            where tiltak_dokument_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()

        execute(
            queryOf(
                query,
                mapOf(
                    "id" to tiltakDokument.id,
                    "navn" to tiltakDokument.navn,
                    "tiltakstype_id" to tiltakDokument.tiltakstypeId,
                    "sted_for_gjennomforing" to tiltakDokument.stedForGjennomforing,
                    "arrangor_id" to tiltakDokument.arrangorId,
                    "faneinnhold" to tiltakDokument.faneinnhold?.let { Json.encodeToString(it) },
                    "beskrivelse" to tiltakDokument.beskrivelse,
                    "tiltaksnummer" to tiltakDokument.tiltaksnummer,
                    "sanity_id" to tiltakDokument.sanityId,
                    "publisert" to tiltakDokument.publisert,
                ),
            ),
        )
        batchPreparedNamedStatement(
            upsertAdministrator,
            tiltakDokument.administratorer.map { mapOf("id" to tiltakDokument.id, "nav_ident" to it.value) },
        )
        execute(
            queryOf(
                deleteAdministratorer,
                tiltakDokument.id,
                createArrayOfValue(tiltakDokument.administratorer) { it.value },
            ),
        )

        batchPreparedNamedStatement(
            upsertEnhet,
            tiltakDokument.navEnheter.map { mapOf("id" to tiltakDokument.id, "enhetsnummer" to it.value) },
        )
        execute(
            queryOf(
                deleteEnheter,
                tiltakDokument.id,
                createArrayOfValue(tiltakDokument.navEnheter) { it.value },
            ),
        )

        batchPreparedNamedStatement(
            upsertKontaktperson,
            tiltakDokument.kontaktpersoner.map { mapOf("id" to tiltakDokument.id, "nav_ident" to it.navIdent.value, "beskrivelse" to it.beskrivelse) },
        )
        execute(
            queryOf(
                deleteKontaktpersoner,
                tiltakDokument.id,
                createArrayOfValue(tiltakDokument.kontaktpersoner) { it.navIdent.value },
            ),
        )

        batchPreparedNamedStatement(
            upsertArrangorKontaktperson,
            tiltakDokument.arrangorKontaktpersoner.map { mapOf("tiltak_dokument_id" to tiltakDokument.id, "arrangor_kontaktperson_id" to it) },
        )
        execute(
            queryOf(
                deleteArrangorKontaktpersoner,
                tiltakDokument.id,
                createUuidArray(tiltakDokument.arrangorKontaktpersoner),
            ),
        )
    }

    override fun getAllKompaktDto(
        navEnheter: List<NavEnhetNummer>,
        tiltakstyper: List<Tiltakskode>,
        publisert: Boolean?,
    ): List<TiltakDokumentKompaktDto> = with(session) {
        @Language("PostgreSQL")
        val query = """
        select *
        from view_tiltak_dokument
        where (:nav_enheter::text[] is null or id in (
            select tiltak_dokument_id from tiltak_dokument_nav_enhet
            where enhetsnummer = any (:nav_enheter)
        ))
        and (:tiltakskoder::text[] is null or tiltakstype_tiltakskode = any (:tiltakskoder))
        and (:publisert::boolean is null or publisert = :publisert)
        order by created_at desc
        """.trimIndent()

        val params = mapOf(
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakskoder" to tiltakstyper.ifEmpty { null }?.let { createArrayOfValue(it) { it.name } },
            "publisert" to publisert,
        )

        list(queryOf(query, params), ::toTiltakDokumentKompakt)
    }

    override fun setPublisert(id: UUID, publisert: Boolean) {
        @Language("PostgreSQL")
        val query = """
            update tiltak_dokument
                set publisert  = ?, updated_at = now()
            where id = ?::uuid
        """.trimIndent()
        session.execute(queryOf(query, publisert, id))
    }

    override fun get(id: UUID): TiltakDokument? {
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
                from tiltak_dokument_administrator adm
                where adm.tiltak_dokument_id = td.id) as administratorer_json,
               (select jsonb_agg(enhet.enhetsnummer)
                from tiltak_dokument_nav_enhet enhet
                where enhet.tiltak_dokument_id = td.id) as nav_enheter_json,
               (select jsonb_agg(jsonb_build_object(
                       'navIdent', kp.kontaktperson_nav_ident,
                       'beskrivelse', kp.beskrivelse
               ))
                from tiltak_dokument_kontaktperson kp
                where kp.tiltak_dokument_id = td.id) as kontaktpersoner_json,
               (select jsonb_agg(akp.arrangor_kontaktperson_id)
                from tiltak_dokument_arrangor_kontaktperson akp
                where akp.tiltak_dokument_id = td.id) as arrangor_kontaktpersoner_json
        from tiltak_dokument td
        where td.id = :id::uuid or td.sanity_id = :id::uuid
        """.trimIndent()

        return session.single(queryOf(query, mapOf("id" to id)), ::toTiltakDokument)
    }

    private fun toTiltakDokument(row: Row): TiltakDokument = TiltakDokument(
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
            ?.let { Json.decodeFromString<List<TiltakDokument.Kontaktperson>>(it) }
            ?: emptyList(),
        arrangorKontaktpersoner = row.stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<String>>(it).map(UUID::fromString) }
            ?: emptyList(),
    )

    override fun delete(id: UUID) {
        session.execute(queryOf("delete from tiltak_dokument where id = ?::uuid", id))
    }

    override fun getTiltakDokumentDto(id: UUID): TiltakDokumentDto? {
        @Language("PostgreSQL")
        val query = "select * from view_tiltak_dokument where id = :id::uuid"
        return session.single(queryOf(query, mapOf("id" to id)), ::toTiltakDokumentDto)
    }

    private fun toTiltakDokumentDto(row: Row): TiltakDokumentDto {
        val tiltakstypeId = row.uuid("tiltakstype_id")
        val arrangorId = row.uuidOrNull("arrangor_id")

        return TiltakDokumentDto(
            id = row.uuid("id"),
            navn = row.string("navn"),
            sanityId = row.uuidOrNull("sanity_id"),
            tiltaksnummer = row.stringOrNull("tiltaksnummer"),
            tiltakstype = tiltakstypeId.let {
                TiltakDokumentDto.Tiltakstype(
                    id = it,
                    navn = row.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(row.string("tiltakstype_tiltakskode")),
                )
            },
            stedForGjennomforing = row.stringOrNull("sted_for_gjennomforing"),
            arrangor = arrangorId?.let {
                TiltakDokumentDto.Arrangor(
                    id = it,
                    navn = row.string("arrangor_navn"),
                    organisasjonsnummer = row.string("arrangor_organisasjonsnummer"),
                )
            },
            faneinnhold = row.stringOrNull("faneinnhold")?.let { Json.Default.decodeFromString(it) },
            beskrivelse = row.stringOrNull("beskrivelse"),
            publisert = row.boolean("publisert"),
            administratorer = row.stringOrNull("administratorer_json")
                ?.let { Json.decodeFromString<List<TiltakDokumentDto.Administrator>>(it) }
                ?: emptyList(),
            kontorstruktur = row.stringOrNull("nav_enheter_json")
                ?.let { Kontorstruktur.fromNavEnheter(Json.decodeFromString<List<NavEnhetDto>>(it)) }
                ?: emptyList(),
            kontaktpersoner = row.stringOrNull("kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<TiltakDokumentDto.Kontaktperson>>(it) }
                ?: emptyList(),
            arrangorKontaktpersoner = row.stringOrNull("arrangor_kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<TiltakDokumentDto.ArrangorKontaktperson>>(it) }
                ?: emptyList(),
        )
    }

    private fun toTiltakDokumentKompakt(row: Row): TiltakDokumentKompaktDto {
        val tiltakstypeId = row.uuid("tiltakstype_id")
        val arrangorId = row.uuidOrNull("arrangor_id")

        return TiltakDokumentKompaktDto(
            id = row.uuid("id"),
            navn = row.string("navn"),
            tiltaksnummer = row.stringOrNull("tiltaksnummer"),
            tiltakstype = tiltakstypeId.let {
                TiltakDokumentKompaktDto.Tiltakstype(
                    id = it,
                    navn = row.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(row.string("tiltakstype_tiltakskode")),
                )
            },
            arrangor = arrangorId?.let {
                TiltakDokumentKompaktDto.Arrangor(
                    id = it,
                    navn = row.string("arrangor_navn"),
                    organisasjonsnummer = row.string("arrangor_organisasjonsnummer"),
                )
            },
            publisert = row.boolean("publisert"),
            navEnheter = row.stringOrNull("nav_enheter_json")
                ?.let { Kontorstruktur.fromNavEnheter(Json.decodeFromString<List<NavEnhetDto>>(it)) }
                ?.flatMap { it.kontorer.map { it.enhetsnummer } + it.region.enhetsnummer }
                ?: emptyList(),
        )
    }
}
