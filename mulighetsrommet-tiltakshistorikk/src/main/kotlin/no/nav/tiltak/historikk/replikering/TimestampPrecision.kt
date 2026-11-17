package no.nav.tiltak.historikk.replikering

import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal fun Instant.truncatedToMicros(): Instant = truncatedTo(ChronoUnit.MICROS)

internal fun LocalDateTime.truncatedToMicros(): LocalDateTime = truncatedTo(ChronoUnit.MICROS)
