package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe

@Serializable
data class Mutations<T>(
    val mutations: List<Mutation<T>>,
)

@Serializable
class Mutation<T> private constructor(
    val createIfNotExists: T? = null,
    val createOrReplace: T? = null,
    val patch: T? = null,
    val delete: Delete? = null,
) {
    companion object {
        fun <T> createIfNotExists(data: T) = Mutation(createIfNotExists = data)

        fun <T> createOrReplace(data: T) = Mutation(createOrReplace = data)

        fun <T> patch(id: String, set: T) = Mutation(patch = Patch(id = id, set = set))

        fun delete(id: String) = Mutation<Unit>(delete = Delete(id))
    }

    @Serializable
    data class Delete(
        val id: String,
    )

    @Serializable
    data class Patch<T>(
        val id: String,
        val set: T,
    )
}

@Serializable
data class SanityTiltakstypeFields(
    val tiltakstypeNavn: String,
    val innsatsgrupper: Set<Innsatsgruppe>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateSanityTiltaksgjennomforingDto(
    val _id: String,
    @EncodeDefault
    val _type: String = "tiltaksgjennomforing",
    val tiltaksgjennomforingNavn: String,
    val tiltakstype: TiltakstypeRef? = null,
    val tiltaksnummer: Slug? = null,
)

@Serializable
data class SanityTiltaksgjennomforingFields(
    val tiltaksgjennomforingNavn: String,
    val fylke: FylkeRef? = null,
    val enheter: List<EnhetRef>? = null,
    val tiltakstype: TiltakstypeRef? = null,
    val tiltaksnummer: Slug? = null,
) {
    fun toSanityTiltaksgjennomforing(id: String) = CreateSanityTiltaksgjennomforingDto(
        _id = id,
        tiltaksgjennomforingNavn = this.tiltaksgjennomforingNavn,
        tiltakstype = this.tiltakstype,
        tiltaksnummer = this.tiltaksnummer,
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class FylkeRef(
    @EncodeDefault
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)
