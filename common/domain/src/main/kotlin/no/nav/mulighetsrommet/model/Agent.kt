package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

sealed interface Agent

@Serializable
data object Tiltaksadministrasjon : Agent

@Serializable
data object Arena : Agent

@Serializable
data object Arrangor : Agent

fun String.toAgent(): Agent = when (this) {
    "Tiltaksadministrasjon" -> Tiltaksadministrasjon
    "Arena" -> Arena
    "Arrangor" -> Arrangor
    else -> NavIdent(this)
}

/* Format used when stored in text column in db */
fun Agent.textRepr(): String = when (this) {
    Arena,
    Arrangor,
    Tiltaksadministrasjon,
    ->
        this.toString()
    is NavIdent -> this.value
}
