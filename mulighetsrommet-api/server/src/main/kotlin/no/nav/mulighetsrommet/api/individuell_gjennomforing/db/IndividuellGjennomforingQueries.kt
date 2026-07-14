package no.nav.mulighetsrommet.api.individuell_gjennomforing.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.admin.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.individuell_gjennomforing.model.IndividuellGjennomforing
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.UUID

class IndividuellGjennomforingQueries(private val session: Session) {

    fun upsert(
        id: UUID,
        navn: String,
        tiltakstypeId: UUID?,
        stedForGjennomforing: String?,
        arrangorId: UUID?,
        faneinnhold: Faneinnhold?,
        beskrivelse: String?,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into individuell_gjennomforing (id, navn, tiltakstype_id, sted_for_gjennomforing, arrangor_id, faneinnhold, beskrivelse)
            values (:id::uuid, :navn, :tiltakstype_id::uuid, :sted_for_gjennomforing, :arrangor_id::uuid, :faneinnhold::jsonb, :beskrivelse)
            on conflict (id) do update set
                navn                   = excluded.navn,
                tiltakstype_id         = excluded.tiltakstype_id,
                sted_for_gjennomforing = excluded.sted_for_gjennomforing,
                arrangor_id            = excluded.arrangor_id,
                faneinnhold            = excluded.faneinnhold,
                beskrivelse            = excluded.beskrivelse,
                updated_at             = now()
        """.trimIndent()

        session.execute(
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "navn" to navn,
                    "tiltakstype_id" to tiltakstypeId,
                    "sted_for_gjennomforing" to stedForGjennomforing,
                    "arrangor_id" to arrangorId,
                    "faneinnhold" to faneinnhold?.let { Json.encodeToString(it) },
                    "beskrivelse" to beskrivelse,
                ),
            ),
        )
    }

    fun setAdministratorer(id: UUID, administratorer: Set<NavIdent>) = with(session) {
        @Language("PostgreSQL")
        val upsertAdministrator = """
            insert into individuell_gjennomforing_administrator (gjennomforing_id, nav_ident)
            values (:id::uuid, :nav_ident)
            on conflict (gjennomforing_id, nav_ident) do nothing
        """.trimIndent()
        batchPreparedNamedStatement(
            upsertAdministrator,
            administratorer.map { mapOf("id" to id, "nav_ident" to it.value) },
        )

        @Language("PostgreSQL")
        val deleteAdministratorer = """
            delete from individuell_gjennomforing_administrator
            where gjennomforing_id = ?::uuid and not (nav_ident = any (?))
        """.trimIndent()
        execute(queryOf(deleteAdministratorer, id, createArrayOfValue(administratorer) { it.value }))
    }

    fun setNavEnheter(id: UUID, navEnheter: Set<NavEnhetNummer>) = with(session) {
        @Language("PostgreSQL")
        val upsertEnhet = """
            insert into individuell_gjennomforing_nav_enhet (gjennomforing_id, enhetsnummer)
            values (:id::uuid, :enhetsnummer)
            on conflict (gjennomforing_id, enhetsnummer) do nothing
        """.trimIndent()
        batchPreparedNamedStatement(upsertEnhet, navEnheter.map { mapOf("id" to id, "enhetsnummer" to it.value) })

        @Language("PostgreSQL")
        val deleteEnheter = """
            delete from individuell_gjennomforing_nav_enhet
            where gjennomforing_id = ?::uuid and not (enhetsnummer = any (?))
        """.trimIndent()
        execute(queryOf(deleteEnheter, id, createArrayOfValue(navEnheter) { it.value }))
    }

    fun setKontaktpersoner(id: UUID, kontaktpersoner: Set<KontaktpersonDbo>) = with(session) {
        @Language("PostgreSQL")
        val upsertKontaktperson = """
            insert into individuell_gjennomforing_kontaktperson (gjennomforing_id, kontaktperson_nav_ident, beskrivelse)
            values (:id::uuid, :nav_ident, :beskrivelse)
            on conflict (gjennomforing_id, kontaktperson_nav_ident) do update set
                beskrivelse = :beskrivelse
        """.trimIndent()
        batchPreparedNamedStatement(
            upsertKontaktperson,
            kontaktpersoner.map { mapOf("id" to id, "nav_ident" to it.navIdent.value, "beskrivelse" to it.beskrivelse) },
        )

        @Language("PostgreSQL")
        val deleteKontaktpersoner = """
            delete from individuell_gjennomforing_kontaktperson
            where gjennomforing_id = ?::uuid and not (kontaktperson_nav_ident = any (?))
        """.trimIndent()
        execute(queryOf(deleteKontaktpersoner, id, createArrayOfValue(kontaktpersoner) { it.navIdent.value }))
    }

    fun setArrangorKontaktpersoner(id: UUID, arrangorKontaktpersoner: Set<UUID>) = with(session) {
        @Language("PostgreSQL")
        val upsertArrangorKontaktperson = """
            insert into individuell_gjennomforing_arrangor_kontaktperson (gjennomforing_id, arrangor_kontaktperson_id)
            values (:gjennomforing_id::uuid, :arrangor_kontaktperson_id::uuid)
            on conflict do nothing
        """.trimIndent()
        batchPreparedNamedStatement(
            upsertArrangorKontaktperson,
            arrangorKontaktpersoner.map { mapOf("gjennomforing_id" to id, "arrangor_kontaktperson_id" to it) },
        )

        @Language("PostgreSQL")
        val deleteArrangorKontaktpersoner = """
            delete from individuell_gjennomforing_arrangor_kontaktperson
            where gjennomforing_id = ?::uuid and not (arrangor_kontaktperson_id = any (?))
        """.trimIndent()
        execute(queryOf(deleteArrangorKontaktpersoner, id, createUuidArray(arrangorKontaktpersoner)))
    }

    fun getAll(
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstyper: List<UUID> = emptyList(),
    ): List<IndividuellGjennomforing> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_individuell_gjennomforing
            where (:nav_enheter::text[] is null or id in (
                select gjennomforing_id from individuell_gjennomforing_nav_enhet
                where enhetsnummer = any (:nav_enheter)
            ))
            and (:tiltakstype_ids::uuid[] is null or tiltakstype_id = any (:tiltakstype_ids))
            order by created_at desc
        """.trimIndent()

        val params = mapOf(
            "nav_enheter" to navEnheter.ifEmpty { null }?.let { createArrayOfValue(it) { it.value } },
            "tiltakstype_ids" to tiltakstyper.ifEmpty { null }?.let { createUuidArray(it) },
        )

        list(queryOf(query, params), ::toIndividuellGjennomforing)
    }

    fun get(id: UUID): IndividuellGjennomforing? {
        @Language("PostgreSQL")
        val query = "select * from view_individuell_gjennomforing where id = :id::uuid"
        return session.single(queryOf(query, mapOf("id" to id)), ::toIndividuellGjennomforing)
    }

    private fun toIndividuellGjennomforing(row: Row): IndividuellGjennomforing {
        val tiltakstypeId = row.uuidOrNull("tiltakstype_id")
        val arrangorId = row.uuidOrNull("arrangor_id")

        return IndividuellGjennomforing(
            id = row.uuid("id"),
            navn = row.string("navn"),
            tiltakstype = tiltakstypeId?.let {
                IndividuellGjennomforing.Tiltakstype(
                    id = it,
                    navn = row.string("tiltakstype_navn"),
                    tiltakskode = Tiltakskode.valueOf(row.string("tiltakstype_tiltakskode")),
                )
            },
            stedForGjennomforing = row.stringOrNull("sted_for_gjennomforing"),
            arrangor = arrangorId?.let {
                IndividuellGjennomforing.Arrangor(
                    id = it,
                    navn = row.string("arrangor_navn"),
                    organisasjonsnummer = row.string("arrangor_organisasjonsnummer"),
                )
            },
            faneinnhold = row.stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            beskrivelse = row.stringOrNull("beskrivelse"),
            administratorer = row.stringOrNull("administratorer_json")
                ?.let { Json.decodeFromString<List<IndividuellGjennomforing.Administrator>>(it) }
                ?: emptyList(),
            kontorstruktur = row.stringOrNull("nav_enheter_json")
                ?.let { Kontorstruktur.fromNavEnheter(Json.decodeFromString<List<NavEnhetDto>>(it)) }
                ?: emptyList(),
            kontaktpersoner = row.stringOrNull("kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<IndividuellGjennomforing.Kontaktperson>>(it) }
                ?: emptyList(),
            arrangorKontaktpersoner = row.stringOrNull("arrangor_kontaktpersoner_json")
                ?.let { Json.decodeFromString<List<IndividuellGjennomforing.ArrangorKontaktperson>>(it) }
                ?: emptyList(),
        )
    }

    data class KontaktpersonDbo(
        val navIdent: NavIdent,
        val beskrivelse: String?,
    )
}
