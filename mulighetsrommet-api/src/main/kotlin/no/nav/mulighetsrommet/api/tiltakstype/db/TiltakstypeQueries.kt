package no.nav.mulighetsrommet.api.tiltakstype.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import org.intellij.lang.annotations.Language
import java.util.UUID

class TiltakstypeQueries(private val session: Session) {

    fun upsert(tiltakstype: TiltakstypeDbo) = with(session) {
        @Language("PostgreSQL")
        val query = """
            insert into tiltakstype (
                id,
                navn,
                tiltakskode,
                arena_kode,
                start_dato,
                slutt_dato
            )
            values (
                :id::uuid,
                :navn,
                :tiltakskode,
                :arena_kode,
                :start_dato,
                :slutt_dato
            )
            on conflict (id)
                do update set navn        = excluded.navn,
                              tiltakskode = excluded.tiltakskode,
                              arena_kode = excluded.arena_kode,
                              start_dato = excluded.start_dato,
                              slutt_dato = excluded.slutt_dato
        """.trimIndent()

        execute(queryOf(query, tiltakstype.toSqlParameters()))
    }

    fun setSanityId(id: UUID, sanityId: UUID?) {
        @Language("PostgreSQL")
        val query = """
            update tiltakstype
            set sanity_id = :sanity_id::uuid
            where id = :id::uuid;
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "sanity_id" to sanityId,
        )

        session.execute(queryOf(query, params))
    }

    fun setInnsatsgrupper(id: UUID, innsatsgrupper: Set<Innsatsgruppe>) {
        @Language("PostgreSQL")
        val query = """
            update tiltakstype
            set innsatsgrupper = :innsatsgrupper::innsatsgruppe[]
            where id = :id::uuid;
        """.trimIndent()

        val params = mapOf(
            "id" to id,
            "innsatsgrupper" to session.createArrayOf("innsatsgruppe", innsatsgrupper),
        )

        session.execute(queryOf(query, params))
    }

    fun get(id: UUID): Tiltakstype? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toTiltakstype() }
    }

    fun getEksternTiltakstype(id: UUID): TiltakstypeV3Dto? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, innsatsgrupper, created_at, updated_at
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        val deltakerRegistreringInnhold = getDeltakerregistreringInnhold(id)

        return single(queryOf(query, id)) { it.tiltakstypeEksternDto(deltakerRegistreringInnhold) }
    }

    fun getByTiltakskode(tiltakskode: Tiltakskode): Tiltakstype = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tiltakstype
            where tiltakskode = ?
        """.trimIndent()

        val tiltakstype = single(queryOf(query, tiltakskode.name)) { it.toTiltakstype() }

        return requireNotNull(tiltakstype) {
            "Det finnes ingen tiltakstype for tiltakskode $tiltakskode"
        }
    }

    fun getByArenaTiltakskode(arenaTiltakskode: String): List<Tiltakstype> = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_tiltakstype
            where arena_kode = ?
        """.trimIndent()

        return list(queryOf(query, arenaTiltakskode)) { it.toTiltakstype() }
    }

    fun getAll(
        tiltakskoder: Set<Tiltakskode> = setOf(),
        statuser: List<TiltakstypeStatus> = emptyList(),
        sortering: String? = null,
    ): List<Tiltakstype> = with(session) {
        val parameters = mapOf(
            "tiltakskoder" to tiltakskoder.ifEmpty { null }?.let { createTextArray(it) },
            "statuser" to statuser.ifEmpty { null }?.let { createTextArray(it) },
        )

        val order = when (sortering) {
            "navn-ascending" -> "navn asc"
            "navn-descending" -> "navn desc"
            "startdato-ascending" -> "start_dato asc"
            "startdato-descending" -> "start_dato desc"
            "sluttdato-ascending" -> "slutt_dato asc"
            "sluttdato-descending" -> "slutt_dato desc"
            else -> "navn asc"
        }

        @Language("PostgreSQL")
        val query = """
            select id, navn, tiltakskode, arena_kode, start_dato, slutt_dato, sanity_id, innsatsgrupper, status
            from view_tiltakstype
            where (:tiltakskoder::text[] is null or tiltakskode = any(:tiltakskoder))
              and (:statuser::text[] is null or status = any(:statuser))
            order by $order
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toTiltakstype() }
    }

    fun getVeilederinfo(id: UUID): TiltakstypeVeilderinfo? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select beskrivelse, faneinnhold, faglenker, kan_kombineres_med
            from view_tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toVeilederinfo() }
    }

    fun getAllInnholdselementer(): List<Innholdselement> = with(session) {
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

    fun upsertDeltakerRegistreringInnhold(id: UUID, ledetekst: String?, innholdskoder: List<String>) = with(session) {
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
            where tiltakskode = (select tiltakskode from tiltakstype where id = ?::uuid)
        """.trimIndent()

        execute(queryOf(deleteQuery, id))

        if (innholdskoder.isNotEmpty()) {
            @Language("PostgreSQL")
            val insertQuery = """
                insert into tiltakstype_deltaker_registrering_innholdselement (innholdskode, tiltakskode)
                select :innholdskode, tiltakskode from tiltakstype where id = :id::uuid
                on conflict do nothing
            """.trimIndent()

            innholdskoder.forEach { innholdskode ->
                execute(queryOf(insertQuery, mapOf("innholdskode" to innholdskode, "id" to id)))
            }
        }
    }

    fun getDeltakerregistreringInnhold(id: UUID): DeltakerRegistreringInnholdDto? = with(session) {
        @Language("PostgreSQL")
        val query = """
           select tiltakstype.deltaker_registrering_ledetekst, element.innholdskode, element.tekst
           from tiltakstype
               left join tiltakstype_deltaker_registrering_innholdselement tiltakstype_innholdselement on tiltakstype_innholdselement.tiltakskode = tiltakstype.tiltakskode
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

    fun upsertRedaksjoneltInnhold(
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

    fun setFaglenker(id: UUID, lenker: List<UUID>) {
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

    fun setKanKombineresMed(id: UUID, kombineresmedIds: List<UUID>) = with(session) {
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

    fun delete(id: UUID): Int = with(session) {
        @Language("PostgreSQL")
        val query = """
            delete
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return update(queryOf(query, id))
    }

    private fun TiltakstypeDbo.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode.name,
        "arena_kode" to arenaKode,
        "start_dato" to startDato,
        "slutt_dato" to sluttDato,
    )

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
            startDato = localDate("start_dato"),
            sluttDato = localDateOrNull("slutt_dato"),
            sanityId = uuidOrNull("sanity_id"),
            status = TiltakstypeStatus.valueOf(string("status")),
        )
    }

    private fun Row.toVeilederinfo(): TiltakstypeVeilderinfo {
        val kanKombineresMed = stringOrNull("kan_kombineres_med")
            ?.let { Json.decodeFromString<List<String>>(it) }
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
}
