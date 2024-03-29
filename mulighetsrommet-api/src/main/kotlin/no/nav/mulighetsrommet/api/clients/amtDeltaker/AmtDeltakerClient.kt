package no.nav.mulighetsrommet.api.clients.amtDeltaker

import arrow.core.Either
import arrow.core.left
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog
import java.time.LocalDate
import java.util.*

class AmtDeltakerClient(
    private val baseUrl: String,
    private val tokenProvider: (obo: AccessType.OBO) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = SecureLog.logger
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun hentDeltakelser(
        requestBody: DeltakelserRequest,
        obo: AccessType.OBO,
    ): Either<AmtDeltakerError, DeltakelserResponse> {
        val response = client.post("$baseUrl/deltakelser") {
            bearerAuth(tokenProvider.invoke(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(requestBody)
        }

        return when (response.status) {
            HttpStatusCode.OK -> Either.Right(response.body<DeltakelserResponse>())
            HttpStatusCode.NotFound -> AmtDeltakerError.Error.left()
            HttpStatusCode.BadRequest -> AmtDeltakerError.BadRequest.left()
            else -> {
                val bodyAsText = response.bodyAsText()
                log.error("Feil ved henting av deltakelser for bruker. Response=$bodyAsText")
                AmtDeltakerError.Error.left()
            }
        }
    }
}

enum class AmtDeltakerError {
    BadRequest,
    NotFound,
    Error,
}

@Serializable
data class DeltakelserRequest(
    val norskIdent: String,
)

@Serializable
data class DeltakelserResponse(
    val aktive: List<AktivDeltakelse>,
    val historikk: List<HistoriskDeltakelse>,
) {
    @Serializable
    data class Tiltakstype(
        val navn: String,
        val tiltakskode: GruppeTiltakstype,
    )
}

@Serializable
data class AktivDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val innsoktDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sistEndretdato: LocalDate,
    val aktivStatus: AktivStatusType,
    val tittel: String,
    val tiltakstype: DeltakelserResponse.Tiltakstype,
) {
    @Serializable
    enum class AktivStatusType {
        KLADD,
        UTKAST_TIL_PAMELDING,
        VENTER_PA_OPPSTART,
        DELTAR,
        SOKT_INN,
        VURDERES,
        VENTELISTE,
        PABEGYNT_REGISTRERING,
    }
}

@Serializable
data class HistoriskDeltakelse(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val innsoktDato: LocalDate,
    val periode: Periode?,
    val historiskStatus: HistoriskStatus,
    val tittel: String,
    val tiltakstype: DeltakelserResponse.Tiltakstype,
) {
    @Serializable
    data class HistoriskStatus(
        val historiskStatusType: HistoriskStatusType,
        val aarsak: String? = null,
    )

    @Serializable
    enum class HistoriskStatusType {
        AVBRUTT_UTKAST,
        HAR_SLUTTET,
        IKKE_AKTUELL,
        FEILREGISTRERT,
        AVBRUTT,
        FULLFORT,
    }
}

@Serializable
data class Periode(
    @Serializable(with = LocalDateSerializer::class)
    val startdato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate?,
)

@Serializable
enum class GruppeTiltakstype {
    INDOPPFAG,
    ARBFORB,
    AVKLARAG,
    VASV,
    ARBRRHDAG,
    DIGIOPPARB,
    JOBBK,
    GRUPPEAMO,
    GRUFAGYRKE,
}
