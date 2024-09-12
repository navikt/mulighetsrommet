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
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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
                "med bruker (${data.norskIdent.value})",
        )

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

    fun getDeltMedBruker(fnr: NorskIdent, sanityOrGjennomforingId: UUID): QueryResult<DelMedBrukerDbo?> = query {
        @Language("PostgreSQL")
        val query = """
            select * from del_med_bruker
            where
                norsk_ident = :norsk_ident
                and (sanity_id = :id::uuid or tiltaksgjennomforing_id = :id::uuid)
            order by created_at desc limit 1
        """.trimIndent()
        queryOf(query, mapOf("norsk_ident" to fnr.value, "id" to sanityOrGjennomforingId))
            .map { it.toDelMedBruker() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAlleDistinkteTiltakDeltMedBruker(fnr: NorskIdent): QueryResult<List<DelMedBrukerDbo>?> = query {
        @Language("PostgreSQL")
        val query = """
            select distinct on (tiltaksgjennomforing_id, sanity_id) *
            from del_med_bruker
            where norsk_ident = ?
            order by tiltaksgjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()
        queryOf(query, fnr.value)
            .map { it.toDelMedBruker() }
            .asList
            .let { db.run(it) }
    }

    private fun getAlleTiltakDeltMedBruker(fnr: NorskIdent): QueryResult<List<DelMedBrukerDbo>?> = query {
        @Language("PostgreSQL")
        val query = """
            select *
            from del_med_bruker
            where norsk_ident = ?
            order by tiltaksgjennomforing_id, sanity_id, created_at desc;
        """.trimIndent()
        queryOf(query, fnr.value)
            .map { it.toDelMedBruker() }
            .asList
            .let { db.run(it) }
    }

    suspend fun getDelMedBrukerHistorikk(norskIdent: NorskIdent): Either<Error, List<DeltTiltak>> {
        // Hent delt med bruker-historikk fra database
        val alleTiltakDeltForBruker = getAlleTiltakDeltMedBruker(norskIdent).getOrNull() ?: emptyList()
        val tiltakFraDb = getTiltakFraDb(alleTiltakDeltForBruker)
        val tiltakFraSanity = getTiltakFraSanity(alleTiltakDeltForBruker)

        val alleDelteTiltak = (tiltakFraDb + tiltakFraSanity).sortedBy { it.createdAt }

        return Either.Right(alleDelteTiltak)
    }

    private fun getTiltakFraDb(alleTiltakDelt: List<DelMedBrukerDbo>): List<DeltTiltak> {
        // Slå opp i database for navn
        @Language("PostgreSQL")
        val tiltakFraDbQuery = """
            select tg.navn, tg. id, tt.tiltakskode, tt.navn as tiltakstypeNavn
            from tiltaksgjennomforing tg
            join tiltakstype tt on tt.id = tg.tiltakstype_id
            where tg.id = any(:ids::uuid[])
        """.trimIndent()

        val gjennomforingerIds = alleTiltakDelt
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
                alleTiltakDelt.filter { it.tiltaksgjennomforingId == id }.map {
                    val konstruertNavn = getNavn(tiltak)
                    DeltTiltak(
                        lokaltNavn = tiltak.navn,
                        konstruertNavn = konstruertNavn,
                        createdAt = it.createdAt!!,
                        dialogId = it.dialogId,
                        tiltakId = id.toString(),
                        tiltakstype = DeltTiltak.Tiltakstype(
                            tiltakskode = tiltak.tiltakskode,
                            arenakode = null,
                            navn = tiltak.tiltakstypeNavn,
                        ),
                    )
                }
            }
    }

    private fun getNavn(it: TiltakFraDb): String {
        return if (Tiltakskoder.isKursTiltak(it.tiltakskode) || it.tiltakskode === Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK) {
            it.navn
        } else {
            it.tiltakstypeNavn
        }
    }

    private suspend fun getTiltakFraSanity(alleTiltakDelt: List<DelMedBrukerDbo>): List<DeltTiltak> {
        val sanityQuery = """
         *[_type == "tiltaksgjennomforing" && _id in ${'$'}tiltakIds]{tiltaksgjennomforingNavn, _id, tiltakstype->{_id, tiltakstypeNavn}}
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

        val tiltakFraSanity = when (val result = sanityClient.query(sanityQuery, params)) {
            is SanityResponse.Result -> result.decode<List<SanityTiltak>>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }

        @Language("PostgreSQL")
        val dbQuery = """
            select sanity_id, arena_kode from tiltakstype where sanity_id = any(:ids::uuid[])
        """.trimIndent()

        val sanityTiltakQueryParams =
            mapOf("ids" to tiltakFraSanity.map { UUID.fromString(it.tiltakstype.id) }.let { db.createUuidArray(it) })

        val results = queryOf(dbQuery, sanityTiltakQueryParams)
            .map { it.uuid("sanity_id") to it.string("arena_kode") }
            .asList
            .let { db.run(it) }

        if (results.isEmpty()) {
            return emptyList()
        }

        return tiltakFraSanity.map { tiltak ->
            val arenaKode = results.first { it.first == UUID.fromString(tiltak.tiltakstype.id) }.second
            var konstruertNavn = tiltak.tiltakstype.tiltakstypeNavn

            if (listOf("ENKELAMO", "ENKFAGYRKE").contains(arenaKode)) {
                konstruertNavn = tiltak.navn // Lokalt navn for EnkelAMO og EnkFagYrke
            }

            alleTiltakDelt.filter { it.sanityId == UUID.fromString(tiltak.id) }.map {
                DeltTiltak(
                    lokaltNavn = tiltak.navn,
                    konstruertNavn = konstruertNavn,
                    createdAt = it.createdAt!!,
                    dialogId = it.dialogId,
                    tiltakId = tiltak.id,
                    tiltakstype = DeltTiltak.Tiltakstype(
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
data class DeltTiltak(
    val lokaltNavn: String,
    val konstruertNavn: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val dialogId: String,
    val tiltakId: String,
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
