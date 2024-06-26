package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import io.ktor.server.plugins.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.securelog.SecureLog
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class DelMedBrukerService(private val db: Database, private val sanityClient: SanityClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lagreDelMedBruker(data: DelMedBrukerDbo): QueryResult<DelMedBrukerDbo> = query {
        SecureLog.logger.info(
            "Veileder (${data.navident}) deler tiltak med id: '${data.sanityId ?: data.tiltaksgjennomforingId}' " +
                "med bruker (${data.norskIdent})",
        )

        if (data.norskIdent.trim().length != 11) {
            SecureLog.logger.warn("Brukers fnr er ikke 11 tegn. Innsendt: ${data.norskIdent}")
            throw BadRequestException("Brukers fnr er ikke 11 tegn")
        }

        if (data.navident.trim().isEmpty()) {
            SecureLog.logger.warn(
                "Veileders NAVident er tomt. Kan ikke lagre info om tiltak.",
            )
            throw BadRequestException("Veileders NAVident er ikke 6 tegn")
        }

        if (data.sanityId == null && data.tiltaksgjennomforingId == null) {
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

        queryOf(query, data.toParameters())
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getDeltMedBruker(fnr: String, id: UUID): QueryResult<DelMedBrukerDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select * from del_med_bruker
            where
                norsk_ident = ?
                and (sanity_id = ?::uuid or tiltaksgjennomforing_id = ?::uuid)
            order by created_at desc limit 1
        """.trimIndent()
        queryOf(query, fnr, id.toString(), id)
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAlleDistinkteTiltakDeltMedBruker(fnr: String): QueryResult<List<DelMedBrukerDbo>?> = query {
        @Language("PostgreSQL")
        val query = """
            select distinct on (tiltaksgjennomforing_id, sanity_id) *
            from del_med_bruker
            where norsk_ident = ?
            order by tiltaksgjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()
        queryOf(query, fnr)
            .map { it.toDelMedBruker() }
            .asList
            .let { db.run(it) }
    }

    private fun getAlleTiltakDeltMedBruker(fnr: String): QueryResult<List<DelMedBrukerDbo>?> = query {
        @Language("PostgreSQL")
        val query = """
            select *
            from del_med_bruker
            where norsk_ident = ?
            order by tiltaksgjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()
        queryOf(query, fnr)
            .map { it.toDelMedBruker() }
            .asList
            .let { db.run(it) }
    }

    suspend fun getDelMedBrukerHistorikk(norskIdent: String): Either<Error, List<DeltTiltak>> {
        // Hent delt med bruker-historikk fra database
        val alleTiltakDeltForBruker = getAlleTiltakDeltMedBruker(norskIdent).getOrNull() ?: emptyList()
        val tiltakFraDb = getTiltakFraDb(alleTiltakDeltForBruker)
        val tiltakFraSanity = getTiltakFraSanity(alleTiltakDeltForBruker)

        val alleDelteTiltak = (tiltakFraDb + tiltakFraSanity)

        return Either.Right(alleDelteTiltak)
    }

    private fun getTiltakFraDb(alleTiltakDelt: List<DelMedBrukerDbo>): List<DeltTiltak> {
        // Slå opp i database for navn
        @Language("PostgreSQL")
        val tiltakFraDbQuery = """
            select navn, id
            from tiltaksgjennomforing
            where id = any(:ids::uuid[])
        """.trimIndent()

        val tiltakFraDbParams = mapOf(
            "ids" to alleTiltakDelt.mapNotNull { it.tiltaksgjennomforingId }
                .let { db.createUuidArray(it) },
        )

        return queryOf(tiltakFraDbQuery, tiltakFraDbParams)
            .map { it.uuid("id") to it.string("navn") }
            .asList
            .let { db.run(it) }
            .flatMap { (id, navn) ->
                alleTiltakDelt.filter { it.tiltaksgjennomforingId == id }.map {
                    DeltTiltak(navn, it.createdAt!!, it.dialogId, id.toString())
                }
            }
    }

    private suspend fun getTiltakFraSanity(alleTiltakDelt: List<DelMedBrukerDbo>): List<DeltTiltak> {
        // Slå opp i Sanity for navn og id
        val query = """
         *[_type == "tiltaksgjennomforing" && _id in ${'$'}tiltakIds]{tiltaksgjennomforingNavn, _id}
        """.trimIndent()

        val params = buildList {
            add(
                SanityParam.of(
                    "tiltakIds",
                    alleTiltakDelt.filter { it.sanityId != null }.map {
                        it.sanityId.toString()
                    },
                ),
            )
        }

        return when (val result = sanityClient.query(query, params)) {
            is SanityResponse.Result -> result.decode<List<SanityTiltak>>().flatMap { sanityTiltak ->
                alleTiltakDelt.filter { it.sanityId?.toString() == sanityTiltak.id }.map {
                    DeltTiltak(sanityTiltak.navn, it.createdAt!!, it.dialogId, it.sanityId.toString())
                }
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }
}

private fun DelMedBrukerDbo.toParameters() = mapOf(
    "norsk_ident" to norskIdent,
    "navident" to navident,
    "sanity_id" to sanityId,
    "tiltaksgjennomforing_id" to tiltaksgjennomforingId,
    "dialogid" to dialogId,
    "created_by" to navident,
    "updated_by" to navident,
)

private fun Row.toDelMedBruker(): DelMedBrukerDbo = DelMedBrukerDbo(
    id = string("id"),
    norskIdent = string("norsk_ident"),
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
data class DeltTiltak(
    val navn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val dialogId: String,
    val tiltakId: String,
)

@Serializable
data class SanityTiltak(
    @SerialName("tiltaksgjennomforingNavn")
    val navn: String,
    @SerialName("_id")
    val id: String,
)
