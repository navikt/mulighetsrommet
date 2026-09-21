package no.nav.tiltak.okonomi

import com.diffplug.selfie.kotest.SelfieExtension
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension

object KotestProjectConfig : AbstractProjectConfig() {
    override val extensions: List<Extension> = listOf(SelfieExtension(this))
}
