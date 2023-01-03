package no.nav.mulighetsrommet.api.utils

import java.util.*

fun String.toUUID(): UUID = UUID.fromString(this)
