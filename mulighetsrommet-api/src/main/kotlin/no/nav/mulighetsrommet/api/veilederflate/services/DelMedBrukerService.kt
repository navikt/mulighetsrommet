package no.nav.mulighetsrommet.api.veilederflate.services

import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.sanity.CacheUsage
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.models.DelMedBrukerDbo
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.utils.toUUID
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class DelMedBrukerService(
    private val db: ApiDatabase,
    private val sanityService: SanityService,
    private val tiltakstypeService: TiltakstypeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lagreDelMedBruker(dbo: DelMedBrukerDbo): DelMedBrukerDbo = db.session {
        // TODO flytt til routes?
        SecureLog.logger.info(
            "Veileder (${dbo.navident}) deler tiltak med id: '${dbo.sanityId ?: dbo.gjennomforingId}' " +
                "med bruker (${dbo.norskIdent.value})",
        )

        if (dbo.navident.trim().isEmpty()) {
            SecureLog.logger.warn(
                "Veileders NAVident er tomt. Kan ikke lagre info om tiltak.",
            )
            throw BadRequestException("Veileders NAVident er ikke 6 tegn")
        }

        if (dbo.sanityId == null && dbo.gjennomforingId == null) {
            log.warn("Id til gjennomføringen mangler")
            throw BadRequestException("sanityId eller gjennomforingId må inkluderes")
        }

        if (dbo.deltFraFylke == null) {
            log.warn("Veileder tilhører ikke noe fylke")
            throw BadRequestException("Veileder tilhører ikke noe fylke - Lagrer ikke deling med bruker")
        }

        @Language("PostgreSQL")
        val query = """
            insert into del_med_bruker(
                norsk_ident,
                navident,
                sanity_id,
                dialogid,
                created_by,
                updated_by,
                gjennomforing_id,
                tiltakstype_navn,
                tiltakstype_id,
                delt_fra_fylke,
                delt_fra_enhet
            )
            values (
                :norsk_ident,
                :navident,
                :sanity_id::uuid,
                :dialogid,
                :created_by,
                :updated_by,
                :gjennomforing_id::uuid,
                :tiltakstype_navn,
                :tiltakstype_id::uuid,
                :delt_fra_fylke,
                :delt_fra_enhet
            )
            returning *
        """.trimIndent()

        val tiltakstype = queries.tiltakstype.getAll().singleOrNull {
            it.status == TiltakstypeStatus.AKTIV && it.navn == dbo.tiltakstypeNavn
        }

        val params = mapOf(
            "norsk_ident" to dbo.norskIdent.value,
            "navident" to dbo.navident,
            "sanity_id" to dbo.sanityId,
            "gjennomforing_id" to dbo.gjennomforingId,
            "dialogid" to dbo.dialogId,
            "created_by" to dbo.navident,
            "updated_by" to dbo.navident,
            "tiltakstype_navn" to dbo.tiltakstypeNavn,
            "tiltakstype_id" to tiltakstype?.id,
            "delt_fra_fylke" to dbo.deltFraFylke?.value,
            "delt_fra_enhet" to dbo.deltFraEnhet?.value,
        )

        session.requireSingle(queryOf(query, params)) { it.toDelMedBruker() }
    }

    fun getDeltMedBruker(fnr: NorskIdent, sanityOrGjennomforingId: UUID): DelMedBrukerDbo? = db.session {
        @Language("PostgreSQL")
        val query = """
            select *
            from del_med_bruker
            where norsk_ident = :norsk_ident
              and (sanity_id = :id::uuid or gjennomforing_id = :id::uuid)
            order by created_at desc
            limit 1
        """.trimIndent()

        val params = mapOf("norsk_ident" to fnr.value, "id" to sanityOrGjennomforingId)

        session.single(queryOf(query, params)) { it.toDelMedBruker() }
    }

    fun getAlleDistinkteTiltakDeltMedBruker(fnr: NorskIdent): List<DelMedBrukerDbo> = db.session {
        @Language("PostgreSQL")
        val query = """
            select distinct on (gjennomforing_id, sanity_id) *
            from del_med_bruker
            where norsk_ident = ?
            order by gjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()

        session.list(queryOf(query, fnr.value)) { it.toDelMedBruker() }
    }

    private fun getAlleTiltakDeltMedBruker(fnr: NorskIdent): List<DelMedBrukerDbo> = db.session {
        @Language("PostgreSQL")
        val query = """
            select *
            from del_med_bruker
            where norsk_ident = ?
            order by gjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()

        session.list(queryOf(query, fnr.value)) { it.toDelMedBruker() }
    }

    suspend fun getDelMedBrukerHistorikk(norskIdent: NorskIdent): List<TiltakDeltMedBruker> {
        // Hent delt med bruker-historikk fra database
        val alleDeltMedBruker = getAlleTiltakDeltMedBruker(norskIdent)
        val tiltakFraDb = getTiltakFraDb(alleDeltMedBruker)
        val tiltakFraSanity = getTiltakFraSanity(alleDeltMedBruker)

        val alleDelteTiltak = (tiltakFraDb + tiltakFraSanity).sortedBy { it.createdAt }

        return alleDelteTiltak
    }

    private fun getTiltakFraDb(deltMedBruker: List<DelMedBrukerDbo>): List<TiltakDeltMedBruker> = db.session {
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
                createdAt = deling.createdAt!!,
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

    private suspend fun getTiltakFraSanity(deltMedBruker: List<DelMedBrukerDbo>): List<TiltakDeltMedBruker> {
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
                    createdAt = it.createdAt!!,
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

private fun Row.toDelMedBruker(): DelMedBrukerDbo = DelMedBrukerDbo(
    id = string("id"),
    norskIdent = NorskIdent(string("norsk_ident")),
    navident = string("navident"),
    sanityId = uuidOrNull("sanity_id"),
    gjennomforingId = uuidOrNull("gjennomforing_id"),
    dialogId = string("dialogid"),
    createdAt = localDateTime("created_at"),
    updatedAt = localDateTime("updated_at"),
    createdBy = string("created_by"),
    updatedBy = string("updated_by"),
    tiltakstypeNavn = stringOrNull("tiltakstype_navn"),
    deltFraFylke = stringOrNull("delt_fra_fylke")?.let { NavEnhetNummer(it) },
    deltFraEnhet = stringOrNull("delt_fra_enhet")?.let { NavEnhetNummer(it) },
)

@Serializable
data class TiltakDeltMedBruker(
    val navn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val dialogId: String,
    @Serializable(with = UUIDSerializer::class)
    val tiltakId: UUID,
    val tiltakstype: Tiltakstype,
) {
    @Serializable
    data class Tiltakstype(
        val tiltakskode: Tiltakskode?,
        val arenakode: String?,
        val navn: String,
    )
}

data class TiltakFraDb(
    val navn: String,
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val tiltakstypeNavn: String,
)
