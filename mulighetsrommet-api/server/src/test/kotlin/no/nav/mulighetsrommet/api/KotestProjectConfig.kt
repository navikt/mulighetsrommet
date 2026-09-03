package no.nav.mulighetsrommet.api

import com.diffplug.selfie.kotest.SelfieExtension
import io.kotest.core.extensions.Extension
import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig) {
    override val extensions: List<Extension> = listOf(SelfieExtension(this))
}
