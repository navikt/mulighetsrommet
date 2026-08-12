package no.nav.mulighetsrommet.api.totrinnskontroll.api

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

/**
 * Body for beslutter/attestant-endepunkter uten årsaker (godkjenn, attester, ...).
 * `totrinnskontrollId` må være id-en til den totrinnskontrollen som faktisk er til behandling,
 * slik at klienten ikke kan beslutte på et utdatert grunnlag den ikke har sett oppdatert.
 */
@Serializable
data class BeslutningRequest(
    @Serializable(with = UUIDSerializer::class)
    val totrinnskontrollId: UUID,
)

/**
 * Body for beslutter/attestant-endepunkter som også krever årsaker og forklaring (returner, ...).
 * Se [BeslutningRequest] for hvorfor `totrinnskontrollId` er påkrevd.
 */
@Serializable
data class BeslutningMedAarsakerRequest<T>(
    @Serializable(with = UUIDSerializer::class)
    val totrinnskontrollId: UUID,
    val aarsaker: List<T>,
    val forklaring: String?,
) {
    fun validate(): Either<List<FieldError>, BeslutningMedAarsakerRequest<T>> = AarsakerOgForklaringRequest(aarsaker, forklaring).validate().map { this }
}

@Serializable
data class SettPaVentRequest(
    @Serializable(with = UUIDSerializer::class)
    val totrinnskontrollId: UUID,
    val forklaring: String? = null,
)
