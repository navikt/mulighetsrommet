package no.nav.mulighetsrommet.api.veilederflate.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.sanity.CacheUsage
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.veilederflate.models.*
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.securelog.SecureLog
import org.intellij.lang.annotations.Language
import java.util.*

class DelMedBrukerService(
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val navEnhetService: NavEnhetService,
) {
    fun insertDelMedBruker(dbo: DelMedBrukerDbo): Unit = db.session {
        SecureLog.logger.info(
            "Veileder (${dbo.navIdent}) deler tiltak med id: '${dbo.sanityId ?: dbo.gjennomforingId}' med bruker (${dbo.norskIdent.value})",
        )

        val fylke = navEnhetService.hentOverordnetFylkesenhet(dbo.deltFraEnhet)

        @Language("PostgreSQL")
        val query = """
            insert into del_med_bruker(
                norsk_ident,
                nav_ident,
                sanity_id,
                dialog_id,
                gjennomforing_id,
                tiltakstype_id,
                delt_fra_fylke,
                delt_fra_enhet
            )
            values (
                :norsk_ident,
                :nav_ident,
                :sanity_id::uuid,
                :dialog_id,
                :gjennomforing_id::uuid,
                :tiltakstype_id,
                :delt_fra_fylke,
                :delt_fra_enhet
            )
        """.trimIndent()

        val params = mapOf(
            "norsk_ident" to dbo.norskIdent.value,
            "nav_ident" to dbo.navIdent.value,
            "sanity_id" to dbo.sanityId,
            "gjennomforing_id" to dbo.gjennomforingId,
            "dialog_id" to dbo.dialogId,
            "tiltakstype_id" to dbo.tiltakstypeId,
            "delt_fra_enhet" to dbo.deltFraEnhet.value,
            "delt_fra_fylke" to fylke?.enhetsnummer?.value,
        )

        session.execute(queryOf(query, params))
    }

    fun getLastDelingMedBruker(fnr: NorskIdent, sanityOrGjennomforingId: UUID): DeltMedBrukerDto? = db.session {
        @Language("PostgreSQL")
        val query = """
            select coalesce(gjennomforing_id, sanity_id) as tiltak_id, dialog_id, created_at
            from del_med_bruker
            where norsk_ident = :norsk_ident
              and coalesce(gjennomforing_id, sanity_id) = :id::uuid
            order by created_at desc
            limit 1
        """.trimIndent()

        val params = mapOf("norsk_ident" to fnr.value, "id" to sanityOrGjennomforingId)

        session.single(queryOf(query, params)) { it.toDelMedBruker() }
    }

    fun getAllDistinctDelingMedBruker(fnr: NorskIdent): List<DeltMedBrukerDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select distinct on (gjennomforing_id, sanity_id) coalesce(gjennomforing_id, sanity_id) as tiltak_id, dialog_id, created_at
            from del_med_bruker
            where norsk_ident = ?
            order by gjennomforing_id, sanity_id, created_at desc
        """.trimIndent()

        session.list(queryOf(query, fnr.value)) { it.toDelMedBruker() }
    }

    suspend fun getAllTiltakDeltMedBruker(fnr: NorskIdent): List<TiltakDeltMedBrukerDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select del_med_bruker.id,
                   del_med_bruker.dialog_id,
                   del_med_bruker.created_at,
                   del_med_bruker.sanity_id,
                   del_med_bruker.gjennomforing_id,
                   tiltakstype.navn as tiltakstype_navn,
                   tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                   tiltakstype.arena_kode as tiltakstype_arena_kode,
                   gjennomforing.navn as gjennomforing_navn
            from del_med_bruker
                join tiltakstype on del_med_bruker.tiltakstype_id = tiltakstype.id
                left join gjennomforing on del_med_bruker.gjennomforing_id = gjennomforing.id
            where norsk_ident = ?
        """.trimIndent()

        val tiltakFraSanity = sanityService.getAllTiltak(search = null, CacheUsage.UseCache).associateBy { it._id }

        val historikk = session.list(queryOf(query, fnr.value)) { row ->
            val tiltakstype = TiltakstypeDeltMedBruker(
                tiltakskode = row.stringOrNull("tiltakstype_tiltakskode")?.let { Tiltakskode.valueOf(it) },
                arenakode = row.string("tiltakstype_arena_kode"),
                navn = row.string("tiltakstype_navn"),
            )
            val deling = DelingMedBruker(
                dialogId = row.string("dialog_id"),
                tidspunkt = row.localDateTime("created_at"),
            )
            val tiltak = row.uuidOrNull("gjennomforing_id")
                ?.let { id ->
                    TiltakDeltMedBruker(
                        id = id,
                        navn = row.string("gjennomforing_navn"),
                    )
                }
                ?: run {
                    val id = row.uuid("sanity_id")
                    val navn = tiltakFraSanity[id.toString()]?.tiltaksgjennomforingNavn ?: ""
                    TiltakDeltMedBruker(id, navn)
                }
            TiltakDeltMedBrukerDto(tiltak, deling, tiltakstype)
        }

        historikk.sortedByDescending { it.deling.tidspunkt }
    }
}

private fun Row.toDelMedBruker() = DeltMedBrukerDto(
    tiltakId = uuid("tiltak_id"),
    deling = DelingMedBruker(
        dialogId = string("dialog_id"),
        tidspunkt = localDateTime("created_at"),
    ),
)

data class DelMedBrukerDbo(
    val norskIdent: NorskIdent,
    val navIdent: NavIdent,
    val dialogId: String,
    val tiltakstypeId: UUID,
    val sanityId: UUID?,
    val gjennomforingId: UUID?,
    val deltFraEnhet: NavEnhetNummer,
)
