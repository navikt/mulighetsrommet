package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.*
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
import org.slf4j.Logger

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    install(Koin) {
        SLF4JLogger()
        modules(db(appConfig.database), services(log))
    }
}

private fun db(databaseConfig: DatabaseConfig): Module {
    return module(createdAtStart = true) {
        single { Database(databaseConfig) }
    }
}

private fun services(logger: Logger) = module {
    single { TiltaksgjennomforingService(get()) }
    single { TiltakstypeService(get(), logger) }
    single { InnsatsgruppeService(get()) }
}
