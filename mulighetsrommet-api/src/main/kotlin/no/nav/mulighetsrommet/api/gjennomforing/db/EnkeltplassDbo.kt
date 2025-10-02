package no.nav.mulighetsrommet.api.gjennomforing.db

import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.time.LocalDate
import java.util.*

data class EnkeltplassDbo(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
)

data class EnkeltplassArenaDataDbo(
    val id: UUID,
    val tiltaksnummer: String?,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val arenaAnsvarligEnhet: String,
)
