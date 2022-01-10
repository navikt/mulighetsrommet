package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import io.ktor.application.install
import no.nav.mulighetsrommet.api.database.DatabaseFactory
import no.nav.mulighetsrommet.api.kafka.KafkaFactory
import no.nav.mulighetsrommet.api.services.InnsatsgruppeService
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltaksvariantService
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.koin.logger.SLF4JLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        SLF4JLogger()
        modules(db, kafka, services)
    }
}

private val db = module {
    single { DatabaseFactory() }
}

private val kafka = module {
    single { KafkaFactory(get()) }
}

private val services = module {
    single { TiltaksgjennomforingService(get()) }
    single { TiltaksvariantService(get()) }
    single { InnsatsgruppeService(get()) }
}
