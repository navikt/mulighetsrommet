package no.nav.mulighetsrommet.hoplite

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import no.nav.mulighetsrommet.env.NaisEnv

inline fun <reified T : Any> loadConfiguration(): T = ConfigLoaderBuilder.default()
    .apply {
        when (NaisEnv.current()) {
            NaisEnv.ProdGCP -> {
                addResourceSource("/application-prod.yaml", optional = true)
            }
            NaisEnv.DevGCP -> {
                addResourceSource("/application-dev.yaml", optional = true)
            }
            NaisEnv.Local -> {
                addResourceSource("/application-local.yaml", optional = true)
            }
        }
    }
    .strict()
    .build()
    .loadConfigOrThrow()
