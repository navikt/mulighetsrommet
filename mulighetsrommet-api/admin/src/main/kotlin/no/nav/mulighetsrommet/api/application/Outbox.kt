package no.nav.mulighetsrommet.api.application

import no.nav.mulighetsrommet.model.TiltakstypeV3Dto

interface Outbox {
    fun publish(ekstern: TiltakstypeV3Dto)
}
