package no.nav.mulighetsrommet.utils

import java.util.UUID

fun String.toUUID(): UUID = UUID.fromString(this)
