package no.nav.mulighetsrommet.utils

import java.util.*

fun String.toUUID(): UUID = UUID.fromString(this)
