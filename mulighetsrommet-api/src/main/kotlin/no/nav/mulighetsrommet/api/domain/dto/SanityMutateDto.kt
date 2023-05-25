package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class Mutations<T>(
    val mutations: List<Mutation<T>>,
)

@Serializable
data class Mutation<T>(
    val createIfNotExists: T? = null,
    val createOrReplace: T? = null,
    val patch: T? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SanityTiltaksgjennomforing(
    val _id: String,
    @EncodeDefault
    val _type: String = "tiltaksgjennomforing",
    val tiltaksgjennomforingNavn: String,
    val fylke: FylkeRef? = null,
    val enheter: List<EnhetRef>? = null,
    val tiltakstype: TiltakstypeRef? = null,
    val tiltaksnummer: TiltaksnummerSlug? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate? = null,
    val lokasjon: String? = null,
)

@Serializable
data class SanityEnhet(
    val _id: String,
    val _type: String = "enhet",
    val navn: String,
    val nummer: EnhetSlug,
    val type: String,
    val status: String,
    val fylke: FylkeRef?,
)

@Serializable
data class EnhetSlug(
    val _type: String = "slug",
    val current: String,
)

@Serializable
data class TiltakstypeRef(
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)

@Serializable
data class TiltakstypeIdResponse(
    val _id: String,
)

@Serializable
data class FylkeRef(
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)
