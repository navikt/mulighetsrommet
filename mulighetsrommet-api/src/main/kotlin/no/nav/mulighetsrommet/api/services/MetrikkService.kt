package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.MetrikkRepository
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.metrics.Metrikker
import java.util.concurrent.atomic.AtomicInteger

class MetrikkService(private val metrikkRepository: MetrikkRepository) {
    private val antallUlesteGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.uleste.notifikasjoner", AtomicInteger(0))
    private val antallLesteGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.leste.notifikasjoner", AtomicInteger(0))
    private val antallAvtalerMedAnsvarligGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.med.ansvarlig", AtomicInteger(0))
    private val antallLeverandorer: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.leverandorer", AtomicInteger(0))
    private val antallArrangorer: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.arrangorer", AtomicInteger(0))
    private val antallAnsvarligeForGjennomforingGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.tiltaksgjennomforinger.med.ansvarlig", AtomicInteger(0))
    private val antallDeltakereMedOpphavAmt: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.deltakere.fra.komet", AtomicInteger(0))
    private val antallDeltakereMedOpphavArena: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.deltakere.fra.arena", AtomicInteger(0))
    private val antallAvtalerFraAdminFlateGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.fra.adminflate", AtomicInteger(0))
    private val antallAvtalerFraArenaGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.fra.arena", AtomicInteger(0))

    fun oppdaterMetrikker() {
        antallUlesteGauge.set(metrikkRepository.hentAntallUlesteNotifikasjoner())
        antallLesteGauge.set(metrikkRepository.hentAntallLesteNotifikasjoner())
        antallAvtalerMedAnsvarligGauge.set(metrikkRepository.hentAntallAvtalerMedAnsvarlig())
        antallLeverandorer.set(metrikkRepository.hentAntallLeverandorer())
        antallArrangorer.set(metrikkRepository.hentAntallArrangorer())
        antallAnsvarligeForGjennomforingGauge.set(metrikkRepository.hentAntallAnsvarligForTiltaksgjennomforing())
        antallDeltakereMedOpphavAmt.set(metrikkRepository.hentAntallDeltakerMedOpphav(opphav = Deltakeropphav.AMT))
        antallDeltakereMedOpphavArena.set(metrikkRepository.hentAntallDeltakerMedOpphav(opphav = Deltakeropphav.ARENA))
        antallAvtalerFraAdminFlateGauge.set(metrikkRepository.hentAntallAvtalerMedOpphav(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE))
        antallAvtalerFraArenaGauge.set(metrikkRepository.hentAntallAvtalerMedOpphav(opphav = ArenaMigrering.Opphav.ARENA))
    }
}
