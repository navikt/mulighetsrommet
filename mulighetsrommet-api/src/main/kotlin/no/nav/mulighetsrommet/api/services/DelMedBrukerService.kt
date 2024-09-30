package no.nav.mulighetsrommet.api.services

import io.ktor.server.plugins.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.services.cms.CacheUsage
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.utils.TiltaksnavnUtils.tittelOgUnderTittel
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.utils.toUUID
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class DelMedBrukerService(
    private val db: Database,
    private val sanityService: SanityService,
    private val tiltakstypeService: TiltakstypeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lagreDelMedBruker(dbo: DelMedBrukerDbo): QueryResult<DelMedBrukerDbo> = query {
        SecureLog.logger.info(
            "Veileder (${dbo.navident}) deler tiltak med id: '${dbo.sanityId ?: dbo.tiltaksgjennomforingId}' " +
                "med bruker (${dbo.norskIdent.value})",
        )

        if (dbo.navident.trim().isEmpty()) {
            SecureLog.logger.warn(
                "Veileders NAVident er tomt. Kan ikke lagre info om tiltak.",
            )
            throw BadRequestException("Veileders NAVident er ikke 6 tegn")
        }

        if (dbo.sanityId == null && dbo.tiltaksgjennomforingId == null) {
            log.warn("Id til gjennomføringen mangler")
            throw BadRequestException("sanityId eller tiltaksgjennomforingId må inkluderes")
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
                tiltaksgjennomforing_id
            )
            values (
                :norsk_ident,
                :navident,
                :sanity_id::uuid,
                :dialogid,
                :created_by,
                :updated_by,
                :tiltaksgjennomforing_id::uuid
            )
            returning *
        """.trimIndent()

        queryOf(query, dbo.toParameters())
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getDeltMedBruker(fnr: NorskIdent, sanityOrGjennomforingId: UUID): DelMedBrukerDbo? {
        @Language("PostgreSQL")
        val query = """
            select * from del_med_bruker
            where
                norsk_ident = :norsk_ident
                and (sanity_id = :id::uuid or tiltaksgjennomforing_id = :id::uuid)
            order by created_at desc limit 1
        """.trimIndent()

        return queryOf(query, mapOf("norsk_ident" to fnr.value, "id" to sanityOrGjennomforingId))
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAlleDistinkteTiltakDeltMedBruker(fnr: NorskIdent): List<DelMedBrukerDbo> {
        @Language("PostgreSQL")
        val query = """
            select distinct on (tiltaksgjennomforing_id, sanity_id) *
            from del_med_bruker
            where norsk_ident = ?
            order by tiltaksgjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()

        return queryOf(query, fnr.value)
            .map { it.toDelMedBruker() }
            .asList
            .let { db.run(it) }
    }

    private fun getAlleTiltakDeltMedBruker(fnr: NorskIdent): List<DelMedBrukerDbo> {
        @Language("PostgreSQL")
        val query = """
            select *
            from del_med_bruker
            where norsk_ident = ?
            order by tiltaksgjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()

        return queryOf(query, fnr.value)
            .map { it.toDelMedBruker() }
            .asList
            .let { db.run(it) }
    }

    suspend fun getDelMedBrukerHistorikk(norskIdent: NorskIdent): List<TiltakDeltMedBruker> {
        // Hent delt med bruker-historikk fra database
        val alleDeltMedBruker = getAlleTiltakDeltMedBruker(norskIdent)
        val tiltakFraDb = getTiltakFraDb(alleDeltMedBruker)
        val tiltakFraSanity = getTiltakFraSanity(alleDeltMedBruker)

        val alleDelteTiltak = (tiltakFraDb + tiltakFraSanity).sortedBy { it.createdAt }

        return alleDelteTiltak
    }

    private fun getTiltakFraDb(deltMedBruker: List<DelMedBrukerDbo>): List<TiltakDeltMedBruker> {
        // Slå opp i database for navn
        @Language("PostgreSQL")
        val tiltakFraDbQuery = """
            select
                tg.navn,
                tg.id,
                tt.tiltakskode,
                tt.navn as tiltakstypeNavn
            from tiltaksgjennomforing tg
                inner join tiltakstype tt on tt.id = tg.tiltakstype_id
            where tg.id = any(:ids::uuid[])
        """.trimIndent()

        val gjennomforingerIds = deltMedBruker
            .mapNotNull { it.tiltaksgjennomforingId }
            .let { db.createUuidArray(it) }

        val tiltakFraDbParams = mapOf(
            "ids" to gjennomforingerIds,
        )

        return queryOf(tiltakFraDbQuery, tiltakFraDbParams)
            .map {
                it.uuid("id") to TiltakFraDb(
                    it.string("navn"),
                    it.uuid("id"),
                    Tiltakskode.valueOf(it.string("tiltakskode")),
                    it.string("tiltakstypeNavn"),
                )
            }
            .asList
            .let { db.run(it) }
            .flatMap { (id, tiltak) ->
                deltMedBruker.filter { it.tiltaksgjennomforingId == id }.map {
                    val (tittel, underTittel) = tittelOgUnderTittel(
                        tiltak.navn,
                        tiltak.tiltakstypeNavn,
                        tiltak.tiltakskode,
                    )

                    TiltakDeltMedBruker(
                        tittel = tittel,
                        underTittel = underTittel,
                        createdAt = it.createdAt!!,
                        dialogId = it.dialogId,
                        tiltakId = id,
                        tiltakstype = TiltakDeltMedBruker.Tiltakstype(
                            tiltakskode = tiltak.tiltakskode,
                            arenakode = null,
                            navn = tiltak.tiltakstypeNavn,
                        ),
                    )
                }
            }
    }

    private suspend fun getTiltakFraSanity(deltMedBruker: List<DelMedBrukerDbo>): List<TiltakDeltMedBruker> {
        val tiltakFraSanity = sanityService.getAllTiltak(search = null, CacheUsage.UseCache)
            .filter { it._id.toUUID() in deltMedBruker.map { it.sanityId } }

        val tiltakstyper = tiltakFraSanity
            .map {
                tiltakstypeService.getBySanityId(UUID.fromString(it.tiltakstype._id))
            }

        return tiltakFraSanity.map { tiltak ->
            val arenaKode = tiltakstyper.first { it.id == UUID.fromString(tiltak.tiltakstype._id) }.arenaKode
            val (tittel, underTittel) = tittelOgUnderTittel(
                tiltak.tiltaksgjennomforingNavn ?: "",
                tiltak.tiltakstype.tiltakstypeNavn,
                arenaKode,
            )

            deltMedBruker.filter { it.sanityId == tiltak._id.toUUID() }.map {
                TiltakDeltMedBruker(
                    tittel = tittel,
                    underTittel = underTittel,
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
    "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
    "dialogid" to dialogId,
    "created_by" to navident,
    "updated_by" to navident,
)

private fun Row.toDelMedBruker(): DelMedBrukerDbo = DelMedBrukerDbo(
    id = string("id"),
    norskIdent = NorskIdent(string("norsk_ident")),
    navident = string("navident"),
    sanityId = uuidOrNull("sanity_id"),
    tiltaksgjennomforingId = uuidOrNull("tiltaksgjennomforing_id"),
    dialogId = string("dialogid"),
    createdAt = localDateTime("created_at"),
    updatedAt = localDateTime("updated_at"),
    createdBy = string("created_by"),
    updatedBy = string("updated_by"),
)

@Serializable
data class TiltakDeltMedBruker(
    val tittel: String,
    val underTittel: String,
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

@Serializable
data class TiltakFraDb(
    val navn: String,
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val tiltakstypeNavn: String,
)

@Serializable
data class SanityTiltak(
    @SerialName("tiltaksgjennomforingNavn")
    val navn: String,
    @SerialName("_id")
    val id: String,
    val tiltakstype: Tiltakstype,
) {
    @Serializable
    data class Tiltakstype(
        @SerialName("_id")
        val id: String,
        val tiltakstypeNavn: String,
    )
}
