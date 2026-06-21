package no.nav.mulighetsrommet.api.persistence.tiltak

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.application.tiltak.SortDirection
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeKombinasjon
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeSortField
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import org.intellij.lang.annotations.Language
import java.util.UUID

class TiltakstypeQueries(private val session: Session) : TiltakstypeRepository, TiltakstypeQueryHandler {

    override fun upsert(tiltakstype: Tiltakstype): Tiltakstype = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (
                id,
                navn,
                tiltakskode,
                arena_kode,
                sanity_id,
                innsatsgrupper
            )
            values (
                :id::uuid,
                :navn,
                :tiltakskode,
                :arena_kode,
                :sanity_id::uuid,
                :innsatsgrupper::innsatsgruppe[]
            )
            on conflict (id)
                do update set navn           = excluded.navn,
                              tiltakskode    = excluded.tiltakskode,
                              arena_kode     = excluded.arena_kode,
                              sanity_id      = excluded.sanity_id,
                              innsatsgrupper = excluded.innsatsgrupper
        """.trimIndent()

        val params = mapOf(
            "id" to tiltakstype.id,
            "navn" to tiltakstype.navn,
            "tiltakskode" to tiltakstype.tiltakskode.name,
            "arena_kode" to tiltakstype.arenakode,
            "sanity_id" to tiltakstype.sanityId,
            "innsatsgrupper" to createArrayOf("innsatsgruppe", tiltakstype.innsatsgrupper),
        )
        execute(queryOf(query, params))
        return tiltakstype
    }

    override fun get(id: UUID): Tiltakstype? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toTiltakstype() }
    }

    override fun getAll(tiltakskoder: Set<Tiltakskode>): List<Tiltakstype> = with(session) {
        val parameters = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { createTextArray(it) },
        )

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, sanity_id, innsatsgrupper
            from view_tiltakstype
            where (:tiltakskoder::text[] is null or tiltakskode = any(:tiltakskoder))
            order by navn asc
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toTiltakstype() }
    }

    override fun getByKode(kode: Tiltakskode): Tiltakstype? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tiltakstype
            where tiltakskode = ?
        """.trimIndent()

        return single(queryOf(query, kode.name)) { it.toTiltakstype() }
    }

    override fun delete(id: UUID): Unit = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        update(queryOf(query, id))
    }

    override fun getAll(
        tiltakskoder: Set<Tiltakskode>,
        sortField: TiltakstypeSortField,
        sortDirection: SortDirection,
    ): List<Tiltakstype> = with(session) {
        val parameters = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { createTextArray(it) },
        )

        val dir = if (sortDirection == SortDirection.ASC) "asc" else "desc"
        val order = when (sortField) {
            TiltakstypeSortField.NAVN -> "navn $dir"
            TiltakstypeSortField.TILTAKSKODE -> "tiltakskode $dir"
        }

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, sanity_id, innsatsgrupper
            from view_tiltakstype
            where (:tiltakskoder::text[] is null or tiltakskode = any(:tiltakskoder))
            order by $order
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toTiltakstype() }
    }

    override fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype = with(session) {
        return requireNotNull(getByKode(tiltakskode)) {
            "Det finnes ingen tiltakstype for tiltakskode $tiltakskode"
        }
    }

    override fun getEksternTiltakstype(id: UUID): TiltakstypeV3Dto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, innsatsgrupper, created_at, updated_at
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        val deltakerRegistreringInnhold = getDeltakerregistreringInnhold(id)

        return single(queryOf(query, id)) { it.tiltakstypeEksternDto(deltakerRegistreringInnhold) }
    }

    override fun getVeilederinfo(id: UUID): TiltakstypeVeilderinfo? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select beskrivelse, faneinnhold, faglenker, kan_kombineres_med
            from view_tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toVeilederinfo() }
    }

    override fun getAllInnholdselementer(): List<Innholdselement> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select innholdskode, tekst
            from deltaker_registrering_innholdselement
            order by tekst
        """.trimIndent()

        return list(queryOf(query)) {
            Innholdselement(tekst = it.string("tekst"), innholdskode = it.string("innholdskode"))
        }
    }

    override fun upsertDeltakerRegistreringInnhold(id: UUID, ledetekst: String?, innholdskoder: List<String>) = with(session) {
        @Language("PostgreSQL")
        val updateLedetekstQuery = """
            update tiltakstype
            set deltaker_registrering_ledetekst = :ledetekst
            where id = :id::uuid
        """.trimIndent()

        execute(queryOf(updateLedetekstQuery, mapOf("id" to id, "ledetekst" to ledetekst)))

        @Language("PostgreSQL")
        val deleteQuery = """
            delete from tiltakstype_deltaker_registrering_innholdselement
            where tiltakstype_id = ?::uuid
        """.trimIndent()

        execute(queryOf(deleteQuery, id))

        if (innholdskoder.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertQuery = """
                insert into tiltakstype_deltaker_registrering_innholdselement (innholdskode, tiltakstype_id)
                values (:innholdskode, :tiltakstype_id::uuid)
                on conflict do nothing
            """.trimIndent()

            innholdskoder.forEach { innholdskode ->
                execute(queryOf(insertQuery, mapOf("innholdskode" to innholdskode, "tiltakstype_id" to id)))
            }
        }
    }

    override fun getDeltakerregistreringInnhold(id: UUID): DeltakerRegistreringInnholdDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
           select tiltakstype.deltaker_registrering_ledetekst, element.innholdskode, element.tekst
           from tiltakstype
               left join tiltakstype_deltaker_registrering_innholdselement tiltakstype_innholdselement on tiltakstype_innholdselement.tiltakstype_id = tiltakstype.id
               left join deltaker_registrering_innholdselement element on tiltakstype_innholdselement.innholdskode = element.innholdskode
           where tiltakstype.id = ?::uuid and tiltakstype.deltaker_registrering_ledetekst is not null;
        """.trimIndent()

        val result = list(queryOf(query, id)) {
            val ledetekst = it.string("deltaker_registrering_ledetekst")
            val tekst = it.stringOrNull("tekst")
            val innholdskode = it.stringOrNull("innholdskode")
            val innholdselement = if (tekst != null && innholdskode != null) {
                Innholdselement(tekst = tekst, innholdskode = innholdskode)
            } else {
                null
            }
            Pair(ledetekst, innholdselement)
        }

        if (result.isEmpty()) return null

        val innholdselementer = result.mapNotNull { it.second }
        return DeltakerRegistreringInnholdDto(
            ledetekst = result[0].first,
            innholdselementer = innholdselementer,
        )
    }

    override fun upsertRedaksjoneltInnhold(
        id: UUID,
        beskrivelse: String?,
        faneinnhold: Faneinnhold?,
    ) {
        @Language("PostgreSQL")
        val updateQuery = """
            update tiltakstype
            set beskrivelse = :beskrivelse,
                faneinnhold = :faneinnhold::jsonb
            where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "beskrivelse" to beskrivelse,
            "faneinnhold" to faneinnhold.let { Json.encodeToString<Faneinnhold?>(it) },
        )
        session.execute(queryOf(updateQuery, params))
    }

    override fun setFaglenker(id: UUID, lenker: List<UUID>) {
        @Language("PostgreSQL")
        val deleteLinksQuery = """
            delete
            from tiltakstype_faglenke
            where tiltakstype_id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(deleteLinksQuery, id))

        if (lenker.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertLinkQuery = """
                insert into tiltakstype_faglenke (tiltakstype_id, lenke_id, sort_order)
                values (:tiltakstype_id::uuid, :lenke_id::uuid, :sort_order)
            """.trimIndent()

            val params = lenker.mapIndexed { index, lenkeId ->
                mapOf(
                    "tiltakstype_id" to id,
                    "lenke_id" to lenkeId,
                    "sort_order" to index,
                )
            }
            session.batchPreparedNamedStatement(insertLinkQuery, params)
        }
    }

    override fun setKanKombineresMed(id: UUID, kombineresmedIds: List<UUID>) = with(session) {
        @Language("PostgreSQL")
        val deleteQuery = """
            delete from tiltakstype_kombinasjon where tiltakstype_id = ?::uuid
        """.trimIndent()

        execute(queryOf(deleteQuery, id))

        if (kombineresmedIds.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertQuery = """
                insert into tiltakstype_kombinasjon (tiltakstype_id, kombineres_med_id)
                values (:tiltakstype_id::uuid, :kombineres_med_id::uuid)
                on conflict do nothing
            """.trimIndent()

            kombineresmedIds.forEach { kombineresmedId ->
                execute(
                    queryOf(
                        insertQuery,
                        mapOf(
                            "tiltakstype_id" to id,
                            "kombineres_med_id" to kombineresmedId,
                        ),
                    ),
                )
            }
        }
    }

    private fun Row.toTiltakstype(): Tiltakstype {
        val innsatsgrupper = arrayOrNull<String>("innsatsgrupper")
            ?.map { Innsatsgruppe.valueOf(it) }
            ?.toSet()
            ?: emptySet()

        return Tiltakstype(
            id = uuid("id"),
            navn = string("navn"),
            innsatsgrupper = innsatsgrupper,
            arenakode = stringOrNull("arena_kode"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            sanityId = uuidOrNull("sanity_id"),
        )
    }

    private fun Row.toVeilederinfo(): TiltakstypeVeilderinfo {
        val kanKombineresMed = stringOrNull("kan_kombineres_med")
            ?.let { Json.decodeFromString<List<TiltakstypeKombinasjon>>(it) }
            ?: emptyList()

        val faglenker = stringOrNull("faglenker")
            ?.let { Json.decodeFromString<List<RedaksjoneltInnholdLenke>>(it) }
            ?: emptyList()

        return TiltakstypeVeilderinfo(
            beskrivelse = stringOrNull("beskrivelse"),
            faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
            faglenker = faglenker,
            kanKombineresMed = kanKombineresMed,
        )
    }

    private fun Row.tiltakstypeEksternDto(
        deltakerRegistreringInnhold: DeltakerRegistreringInnholdDto?,
    ): TiltakstypeV3Dto {
        val innsatsgrupper = arrayOrNull<String>("innsatsgrupper")
            ?.map { Innsatsgruppe.valueOf(it) }
            ?.toSet()
            ?: emptySet()
        return TiltakstypeV3Dto(
            id = uuid("id"),
            navn = string("navn"),
            tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
            innsatsgrupper = innsatsgrupper,
            arenaKode = stringOrNull("arena_kode"),
            opprettetTidspunkt = localDateTime("created_at"),
            oppdatertTidspunkt = localDateTime("updated_at"),
            deltakerRegistreringInnhold = deltakerRegistreringInnhold,
        )
    }

    override fun getNamesReferencingLenke(lenkeId: UUID): List<String> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select tiltakstype.navn
            from tiltakstype
            join tiltakstype_faglenke faglenke on faglenke.tiltakstype_id = tiltakstype.id
            where faglenke.lenke_id = ?::uuid
            order by tiltakstype.navn
        """.trimIndent()

        return list(queryOf(query, lenkeId)) { it.string("navn") }
    }
}
