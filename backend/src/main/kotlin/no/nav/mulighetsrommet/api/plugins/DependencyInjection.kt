package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltaksvariantService
import org.koin.dsl.module

fun Application.configureDependencyInjection() = module {
    single { DatabaseFactory() }
    single { TiltaksgjennomforingService(get()) }
    single { TiltaksvariantService(get()) }
    single { InnsatsgruppeService(get()) }
}
