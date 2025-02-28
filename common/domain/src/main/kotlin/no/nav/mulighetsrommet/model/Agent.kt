package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

sealed interface Agent

@Serializable
data object Tiltaksadministrasjon : Agent

@Serializable
data object Arena : Agent

@Serializable
data object Arrangor : Agent
