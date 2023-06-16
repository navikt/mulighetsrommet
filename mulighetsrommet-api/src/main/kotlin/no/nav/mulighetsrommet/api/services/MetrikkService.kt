package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.MetrikkRepository
import no.nav.mulighetsrommet.metrics.Metrikker
import java.util.concurrent.atomic.AtomicInteger

class MetrikkService(private val metrikkRepository: MetrikkRepository) {
    private val antallUlesteGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.uleste.notifikasjoner", AtomicInteger(0))
    private val antallLesteGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.leste.notifikasjoner", AtomicInteger(0))

    fun oppdaterMetrikker() {
        antallUlesteGauge.set(metrikkRepository.hentAntallUlesteNotifikasjoner())
        antallLesteGauge.set(metrikkRepository.hentAntallLesteNotifikasjoner())
    }
}
