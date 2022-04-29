package no.nav.mulighetsrommet.arena_ords_proxy

import kotlinx.serialization.Serializable

@Serializable
data class ArenaPersonIdList(
    val personListe: List<PersonFnr>
)

@Serializable
data class PersonFnr(
    val personId: String,
    val fnr: String?
)

@Serializable
data class ArbeidsgiverInfo(
    val bedriftsnr: Int,
    val orgnrMorselskap: Int
)
