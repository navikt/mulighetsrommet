package no.nav.mulighetsrommet.api.tiltakstype.api

import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.ProblemDetail
import java.util.UUID

fun tiltakstypeNotFound(id: UUID): ProblemDetail = NotFound("Det finnes ikke noen tiltakstype med id $id")
