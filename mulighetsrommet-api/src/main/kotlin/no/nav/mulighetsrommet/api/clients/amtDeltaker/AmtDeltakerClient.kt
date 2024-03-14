package no.nav.mulighetsrommet.api.clients.amtDeltaker

import arrow.core.Either
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import java.time.LocalDate
import java.util.*

class AmtDeltakerClient(
    private val baseUrl: String,
    private val tokenProvider: (obo: AccessType.OBO) -> String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    // TODO Feilhåndtering - Bør ikke krasje frontend hvis vi ikke klarer hente historikk, men gi beskjed til frontend om at vi ikke klarte å hente data
    suspend fun hentDeltakelser(requestBody: DeltakelserRequest, obo: AccessType.OBO): Either<AmtDeltakerError, DeltakelserResponse> {
        val response = client.post("$baseUrl/deltakelser") {
            bearerAuth(tokenProvider.invoke(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(requestBody)
        }

        if (response.status == HttpStatusCode.OK) {
            return Either.Right(response.body<DeltakelserResponse>())
        }

        return Either.Left(AmtDeltakerError(error = "Klarte ikke hente deltakelser for bruker"))
    }
}

@Serializable
data class AmtDeltakerError(
    val error: String,
)

// TODO Type opp i openApi.yaml
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
