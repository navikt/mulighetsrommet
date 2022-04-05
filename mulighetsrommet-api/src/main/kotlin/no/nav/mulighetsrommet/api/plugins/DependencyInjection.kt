package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import io.ktor.application.install
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.DatabaseConfig
import no.nav.mulighetsrommet.api.database.Database
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.logger.SLF4JLogger

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    install(Koin) {
        SLF4JLogger()
        modules(db(appConfig.database), services)
    }
}

private fun db(databaseConfig: DatabaseConfig): Module {
    return module(createdAtStart = true) {
        single { Database(databaseConfig) }
    }
}

private val services = module {
    single { TiltaksgjennomforingService(get()) }
    single { TiltakstypeService(get()) }
    single { InnsatsgruppeService(get()) }
}
