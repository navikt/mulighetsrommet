package no.nav.mulighetsrommet.api.veilederflate.services

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.veilederflate.models.DelMedBrukerDto
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.teamLogsInfo
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.UUID

class DelMedBrukerService(
    private val db: ApiDatabase,
    private val navEnhetService: NavEnhetService,
) {
    private val logger = LoggerFactory.getLogger(DelMedBrukerService::class.java)

    fun insertDelMedBruker(dbo: DelMedBrukerDbo): Unit = db.session {
        logger.teamLogsInfo(
            "Veileder (${dbo.navIdent}) deler tiltak med id: '${dbo.tiltakDokumentId ?: dbo.gjennomforingId}' med bruker (${dbo.norskIdent.value})",
        )

        val fylke = navEnhetService.hentOverordnetFylkesenhet(dbo.deltFraEnhet)

        @Language("PostgreSQL")
        val query = """
            insert into del_med_bruker(
                norsk_ident,
                nav_ident,
                tiltak_dokument_id,
                dialog_id,
                gjennomforing_id,
                tiltakstype_id,
                delt_fra_fylke,
                delt_fra_enhet
            )
            values (
                :norsk_ident,
                :nav_ident,
                :tiltak_dokument_id::uuid,
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
            "tiltak_dokument_id" to dbo.tiltakDokumentId,
            "gjennomforing_id" to dbo.gjennomforingId,
            "dialog_id" to dbo.dialogId,
            "tiltakstype_id" to dbo.tiltakstypeId,
            "delt_fra_enhet" to dbo.deltFraEnhet.value,
            "delt_fra_fylke" to fylke?.enhetsnummer?.value,
        )

        session.execute(queryOf(query, params))
    }

    fun getLast(fnr: NorskIdent, tiltakDokumentOrGjennomforingId: UUID): DelMedBrukerDto? = db.session {
        @Language("PostgreSQL")
        val query = """
            select del_med_bruker.id,
                   del_med_bruker.dialog_id,
                   del_med_bruker.created_at,
                   coalesce(del_med_bruker.gjennomforing_id, del_med_bruker.tiltak_dokument_id) as tiltak_id,
                   tiltakstype.navn as tiltakstype_navn,
                   tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                   tiltakstype.arena_kode as tiltakstype_arena_kode,
                   coalesce(gjennomforing.navn, tiltak_dokument.navn) as tiltak_navn
            from del_med_bruker
                join tiltakstype on del_med_bruker.tiltakstype_id = tiltakstype.id
                left join gjennomforing on del_med_bruker.gjennomforing_id = gjennomforing.id
                left join tiltak_dokument on del_med_bruker.tiltak_dokument_id = tiltak_dokument.id
            where norsk_ident = :norsk_ident
                and coalesce(gjennomforing_id, tiltak_dokument_id) = :id::uuid
            order by created_at desc
            limit 1
        """.trimIndent()

        val params = mapOf("norsk_ident" to fnr.value, "id" to tiltakDokumentOrGjennomforingId)

        session.single(queryOf(query, params)) { it.toDelMedBrukerDto() }
    }

    fun getAll(fnr: NorskIdent): List<DelMedBrukerDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select del_med_bruker.id,
                   del_med_bruker.dialog_id,
                   del_med_bruker.created_at,
                   coalesce(del_med_bruker.gjennomforing_id, del_med_bruker.tiltak_dokument_id) as tiltak_id,
                   tiltakstype.navn as tiltakstype_navn,
                   tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                   coalesce(gjennomforing.navn, tiltak_dokument.navn) as tiltak_navn
            from del_med_bruker
                join tiltakstype on del_med_bruker.tiltakstype_id = tiltakstype.id
                left join gjennomforing on del_med_bruker.gjennomforing_id = gjennomforing.id
                left join tiltak_dokument on del_med_bruker.tiltak_dokument_id = tiltak_dokument.id
            where norsk_ident = ?
            order by created_at desc
        """.trimIndent()

        val historikk = session.list(queryOf(query, fnr.value)) { it.toDelMedBrukerDto() }

        historikk.sortedByDescending { it.tidspunkt }
    }
}

private fun Row.toDelMedBrukerDto(): DelMedBrukerDto {
    val tiltakstype = DelMedBrukerDto.Tiltakstype(
        tiltakskode = this.string("tiltakstype_tiltakskode").let { Tiltakskode.valueOf(it) },
        navn = this.string("tiltakstype_navn"),
    )
    val navn = this.stringOrNull("tiltak_navn")
    val tiltak = DelMedBrukerDto.Tiltak(
        id = this.uuid("tiltak_id"),
        navn = navn,
        slettet = navn == null,
    )
    return DelMedBrukerDto(
        tiltak,
        dialogId = this.string("dialog_id"),
        tidspunkt = this.localDateTime("created_at"),
        tiltakstype,
    )
}

data class DelMedBrukerDbo(
    val norskIdent: NorskIdent,
    val navIdent: NavIdent,
    val dialogId: String,
    val tiltakstypeId: UUID,
    val tiltakDokumentId: UUID?,
    val gjennomforingId: UUID?,
    val deltFraEnhet: NavEnhetNummer,
)
