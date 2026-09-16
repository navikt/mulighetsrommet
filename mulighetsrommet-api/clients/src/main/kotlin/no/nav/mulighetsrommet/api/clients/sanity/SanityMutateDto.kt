package no.nav.mulighetsrommet.api.clients.sanity

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

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

        fun unsetPatch(id: String, unset: List<String>) = Mutation(patch = UnsetPatch(id = id, unset = unset))

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

    @Serializable
    data class UnsetPatch(
        val id: String,
        val unset: List<String>,
    )
}

@Serializable
data class SanityTiltakstypeFields(
    val tiltakstypeNavn: String,
)

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

@Serializable
data class EnhetSlug(
    @EncodeDefault
    val _type: String = "slug",
    val current: String,
)

@Serializable
data class TiltakstypeRef(
    @EncodeDefault
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)

@Serializable
data class FylkeRef(
    @EncodeDefault
    val _type: String = "reference",
    val _ref: String,
    val _key: String? = null,
)
