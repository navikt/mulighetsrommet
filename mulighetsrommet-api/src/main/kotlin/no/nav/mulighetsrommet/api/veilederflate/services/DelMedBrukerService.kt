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
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
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

        @Language("PostgreSQL")
        val query = """
            insert into del_med_bruker(
                norsk_ident,
                navident,
                sanity_id,
                dialogid,
                created_by,
                updated_by,
                gjennomforing_id
            )
            values (
                :norsk_ident,
                :navident,
                :sanity_id::uuid,
                :dialogid,
                :created_by,
                :updated_by,
                :gjennomforing_id::uuid
            )
            returning *
        """.trimIndent()

        session.requireSingle(queryOf(query, dbo.toParameters())) { it.toDelMedBruker() }
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

private fun DelMedBrukerDbo.toParameters() = mapOf(
    "norsk_ident" to norskIdent.value,
    "navident" to navident,
    "sanity_id" to sanityId,
    "gjennomforing_id" to gjennomforingId,
    "dialogid" to dialogId,
    "created_by" to navident,
    "updated_by" to navident,
)

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
