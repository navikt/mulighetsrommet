package no.nav.mulighetsrommet.api.tasks

import java.util.*

object FrontendRoutes {

    fun lenkeTilGjennomforing(lenkenavn: String, id: UUID): String {
        return "[$lenkenavn](${tiltaksgjennomforing(id)})"
    }

    fun lenkeTilAvtale(lenkenavn: String, id: UUID): String {
        return "[$lenkenavn](${avtale(id)})"
    }

    private fun tiltaksgjennomforing(id: UUID): String {
        return "/tiltaksgjennomforinger/$id"
    }

    private fun avtale(id: UUID): String {
        return "/avtaler/$id"
    }
}
