package no.nav.mulighetsrommet.api.clients.pdl

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class PdlIdent(val value: String)

@Serializable
data class IdentInformasjon(
    val ident: PdlIdent,
    val gruppe: IdentGruppe,
    val historisk: Boolean,
)

@Serializable
enum class IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID,
}

@Serializable
data class PdlPerson(
    val navn: List<PdlNavn>,
)

@Serializable
data class PdlNavn(
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
)

@Serializable
data class PdlGeografiskTilknytning(
    val gtType: TypeGeografiskTilknytning,
    val gtLand: String? = null,
    val gtKommune: String? = null,
    val gtBydel: String? = null,
)

enum class TypeGeografiskTilknytning {
    BYDEL,
    KOMMUNE,
    UDEFINERT,
    UTLAND,
}

enum class PdlGradering {
    FORTROLIG,
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    UGRADERT,
}

sealed class GeografiskTilknytningResponse {
    data class GtKommune(val value: String) : GeografiskTilknytningResponse()
    data class GtBydel(val value: String) : GeografiskTilknytningResponse()
    data class GtUtland(val value: String?) : GeografiskTilknytningResponse()
    data object GtUdefinert : GeografiskTilknytningResponse()
}

fun PdlGeografiskTilknytning.toGeografiskTilknytningResponse(): GeografiskTilknytningResponse {
    return when (this.gtType) {
        TypeGeografiskTilknytning.BYDEL -> {
            GeografiskTilknytningResponse.GtBydel(requireNotNull(this.gtBydel))
        }

        TypeGeografiskTilknytning.KOMMUNE -> {
            GeografiskTilknytningResponse.GtKommune(requireNotNull(this.gtKommune))
        }

        TypeGeografiskTilknytning.UTLAND -> {
            GeografiskTilknytningResponse.GtUtland(this.gtLand)
        }

        TypeGeografiskTilknytning.UDEFINERT -> {
            GeografiskTilknytningResponse.GtUdefinert
        }
    }
}
