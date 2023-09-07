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
    val delete: Delete? = null,
)

@Serializable
data class Delete(
    val id: String,
)

@Serializable
data class Patch<T>(
    val id: String,
    val set: T,
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
data class SanityTiltaksgjennomforingFields(
    val tiltaksgjennomforingNavn: String,
    val fylke: FylkeRef? = null,
    val enheter: List<EnhetRef>? = null,
    val tiltakstype: TiltakstypeRef? = null,
    val tiltaksnummer: TiltaksnummerSlug? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate? = null,
    val lokasjon: String? = null,
) {
    fun toSanityTiltaksgjennomforing(id: String) = SanityTiltaksgjennomforing(
        _id = id,
        tiltaksgjennomforingNavn = this.tiltaksgjennomforingNavn,
        fylke = this.fylke,
        enheter = this.enheter,
        tiltakstype = this.tiltakstype,
        tiltaksnummer = this.tiltaksnummer,
        sluttdato = this.sluttdato,
        lokasjon = this.lokasjon,
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SanityEnhet(
    val _id: String,
    @EncodeDefault
    val _type: String = "enhet",
    val navn: String,
    val nummer: EnhetSlug,
    val type: String,
    val status: String,
    val fylke: FylkeRef?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EnhetSlug(
    @EncodeDefault
    val _type: String = "slug",
    val current: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TiltakstypeRef(
    @EncodeDefault
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)

@Serializable
data class TiltakstypeIdResponse(
    val _id: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class FylkeRef(
    @EncodeDefault
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)
