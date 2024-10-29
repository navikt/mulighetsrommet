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

sealed class GeografiskTilknytning {
    data class GtKommune(val value: String) : GeografiskTilknytning()
    data class GtBydel(val value: String) : GeografiskTilknytning()
    data class GtUtland(val value: String?) : GeografiskTilknytning()
    data object GtUdefinert : GeografiskTilknytning()
}

enum class TypeGeografiskTilknytning {
    BYDEL,
    KOMMUNE,
    UDEFINERT,
    UTLAND,
}
