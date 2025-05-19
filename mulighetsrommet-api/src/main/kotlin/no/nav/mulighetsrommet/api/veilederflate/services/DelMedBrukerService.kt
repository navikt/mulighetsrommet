package no.nav.mulighetsrommet.api.veilederflate.services

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.sanity.CacheUsage
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.models.DelMedBrukerDto
import no.nav.mulighetsrommet.api.veilederflate.models.TiltakDeltMedBruker
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.utils.toUUID
import org.intellij.lang.annotations.Language
import java.util.*

class DelMedBrukerService(
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val tiltakstypeService: TiltakstypeService,
) {
    fun lagreDelMedBruker(dbo: DelMedBrukerInsertDbo): Unit = db.session {
        SecureLog.logger.info(
            "Veileder (${dbo.navIdent}) deler tiltak med id: '${dbo.sanityId ?: dbo.gjennomforingId}' med bruker (${dbo.norskIdent.value})",
        )

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
            "delt_fra_fylke" to dbo.deltFraFylke?.value,
            "delt_fra_enhet" to dbo.deltFraEnhet?.value,
        )

        session.execute(queryOf(query, params))
    }

    fun getTiltakDeltMedBruker(fnr: NorskIdent, sanityOrGjennomforingId: UUID): DelMedBrukerDto? = db.session {
        @Language("PostgreSQL")
        val query = """
            select id, dialog_id, created_at, sanity_id, gjennomforing_id
            from del_med_bruker
            where norsk_ident = :norsk_ident
              and (sanity_id = :id::uuid or gjennomforing_id = :id::uuid)
            order by created_at desc
            limit 1
        """.trimIndent()

        val params = mapOf("norsk_ident" to fnr.value, "id" to sanityOrGjennomforingId)

        session.single(queryOf(query, params)) { it.toDelMedBruker() }
    }

    fun getAlleDistinkteTiltakDeltMedBruker(fnr: NorskIdent): List<DelMedBrukerDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select distinct on (gjennomforing_id, sanity_id) id, dialog_id, created_at, sanity_id, gjennomforing_id
            from del_med_bruker
            where norsk_ident = ?
            order by gjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()

        session.list(queryOf(query, fnr.value)) { it.toDelMedBruker() }
    }

    private fun getAlleTiltakDeltMedBruker(fnr: NorskIdent): List<DelMedBrukerDto> = db.session {
        @Language("PostgreSQL")
        val query = """
            select id, dialog_id, created_at, sanity_id, gjennomforing_id
            from del_med_bruker
            where norsk_ident = ?
            order by gjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()

        session.list(queryOf(query, fnr.value)) { it.toDelMedBruker() }
    }

    suspend fun getDelMedBrukerHistorikk(norskIdent: NorskIdent): List<TiltakDeltMedBruker> = coroutineScope {
        val alleDeltMedBruker = getAlleTiltakDeltMedBruker(norskIdent)

        val tiltakFraDb = async { getTiltakFraDb(alleDeltMedBruker) }
        val tiltakFraSanity = async { getTiltakFraSanity(alleDeltMedBruker) }

        (tiltakFraDb.await() + tiltakFraSanity.await()).sortedBy { it.createdAt }
    }

    private fun getTiltakFraDb(deltMedBruker: List<DelMedBrukerDto>): List<TiltakDeltMedBruker> = db.session {
        @Language("PostgreSQL")
        val tiltakFraDbQuery = """
            select
                tg.navn,
                tg.id,
                tt.tiltakskode,
                tt.navn as tiltakstypeNavn
            from gjennomforing tg
                inner join tiltakstype tt on tt.id = tg.tiltakstype_id
            where tg.id = any(?::uuid[])
        """.trimIndent()

        val ids = deltMedBruker.mapNotNull { it.gjennomforingId }

        val deltById = deltMedBruker.associateBy { it.gjennomforingId }

        val tiltakById = session.list(queryOf(tiltakFraDbQuery, session.createUuidArray(ids))) {
            TiltakFraDb(
                it.string("navn"),
                it.uuid("id"),
                Tiltakskode.valueOf(it.string("tiltakskode")),
                it.string("tiltakstypeNavn"),
            )
        }.associateBy { it.id }

        ids.mapNotNull { id ->
            val deling = deltById[id] ?: return@mapNotNull null
            val tiltak = tiltakById[id] ?: return@mapNotNull null
            TiltakDeltMedBruker(
                navn = tiltak.navn,
                createdAt = deling.createdAt,
                dialogId = deling.dialogId,
                tiltakId = id,
                tiltakstype = TiltakDeltMedBruker.Tiltakstype(
                    tiltakskode = tiltak.tiltakskode,
                    arenakode = null,
                    navn = tiltak.tiltakstypeNavn,
                ),
            )
        }
    }

    private suspend fun getTiltakFraSanity(deltMedBruker: List<DelMedBrukerDto>): List<TiltakDeltMedBruker> {
        val delteSanityTiltak = deltMedBruker.mapNotNull { deling -> deling.sanityId }

        val tiltakFraSanity = sanityService.getAllTiltak(search = null, CacheUsage.UseCache).filter {
            it._id.toUUID() in delteSanityTiltak
        }

        val tiltakstyper = tiltakFraSanity
            .map { tiltakstypeService.getBySanityId(UUID.fromString(it.tiltakstype._id)) }
            .associateBy { it.sanityId }

        return tiltakFraSanity.map { tiltak ->
            val arenaKode = tiltakstyper.getValue(UUID.fromString(tiltak.tiltakstype._id)).arenaKode

            deltMedBruker.filter { it.sanityId == tiltak._id.toUUID() }.map {
                TiltakDeltMedBruker(
                    navn = tiltak.tiltaksgjennomforingNavn ?: "",
                    createdAt = it.createdAt,
                    dialogId = it.dialogId,
                    tiltakId = tiltak._id.toUUID(),
                    tiltakstype = TiltakDeltMedBruker.Tiltakstype(
                        tiltakskode = null,
                        arenakode = arenaKode,
                        navn = tiltak.tiltakstype.tiltakstypeNavn,
                    ),
                )
            }
        }.flatten()
    }
}

private fun Row.toDelMedBruker(): DelMedBrukerDto = DelMedBrukerDto(
    id = int("id"),
    dialogId = string("dialog_id"),
    createdAt = localDateTime("created_at"),
    sanityId = uuidOrNull("sanity_id"),
    gjennomforingId = uuidOrNull("gjennomforing_id"),
)

data class DelMedBrukerInsertDbo(
    val norskIdent: NorskIdent,
    val navIdent: NavIdent,
    val dialogId: String,
    val sanityId: UUID?,
    val gjennomforingId: UUID?,
    val tiltakstypeId: UUID,
    val deltFraFylke: NavEnhetNummer?,
    val deltFraEnhet: NavEnhetNummer?,
)

data class TiltakFraDb(
    val navn: String,
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val tiltakstypeNavn: String,
)
