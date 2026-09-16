package no.nav.mulighetsrommet.api.persistence.veilederflate

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerDto
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerQueryHandler
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.UUID

class DelMedBrukerQueries(private val session: Session) : DelMedBrukerQueryHandler {

    override fun insert(dbo: DelMedBrukerDbo, deltFraFylke: NavEnhetNummer?) {
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
            "delt_fra_fylke" to deltFraFylke?.value,
        )

        session.execute(queryOf(query, params))
    }

    override fun getLast(norskIdent: NorskIdent, tiltakDokumentOrGjennomforingId: UUID): DelMedBrukerDto? {
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

        val params = mapOf("norsk_ident" to norskIdent.value, "id" to tiltakDokumentOrGjennomforingId)

        return session.single(queryOf(query, params)) { it.toDelMedBrukerDto() }
    }

    override fun getAll(norskIdent: NorskIdent): List<DelMedBrukerDto> {
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

        return session.list(queryOf(query, norskIdent.value)) { it.toDelMedBrukerDto() }
    }
}

private fun Row.toDelMedBrukerDto(): DelMedBrukerDto {
    val tiltakstype = DelMedBrukerDto.Tiltakstype(
        tiltakskode = this.string("tiltakstype_tiltakskode").let { Tiltakskode.valueOf(it) },
        navn = this.string("tiltakstype_navn"),
    )
    val id = this.uuidOrNull("tiltak_id")
    val tiltak = DelMedBrukerDto.Tiltak(
        id = id,
        navn = this.stringOrNull("tiltak_navn"),
        slettet = id == null,
    )
    return DelMedBrukerDto(
        tiltak,
        dialogId = this.string("dialog_id"),
        tidspunkt = this.localDateTime("created_at"),
        tiltakstype,
    )
}
