package no.nav.mulighetsrommet.arena_ords_proxy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArenaPersonIdList(
    val personListe: List<PersonFnr>
)

@Serializable
data class PersonFnr(
    val personId: Int,
    val fnr: String? = null
)

@Serializable
data class ArbeidsgiverInfo(
    val bedriftsnr: Int,
    val orgnrMorselskap: Int
)
