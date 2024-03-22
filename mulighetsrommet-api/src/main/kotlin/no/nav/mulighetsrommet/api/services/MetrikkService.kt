package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.repositories.MetrikkRepository
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.metrics.Metrikker
import java.util.concurrent.atomic.AtomicInteger

class MetrikkService(private val metrikkRepository: MetrikkRepository) {
    private val antallUlesteGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.uleste.notifikasjoner", AtomicInteger(0))!!
    private val antallLesteGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.leste.notifikasjoner", AtomicInteger(0))!!
    private val antallAvtalerMedAdministratorGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.med.administrator", AtomicInteger(0))!!
    private val antallGjennomforingerMedAdministratorGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.tiltaksgjennomforinger.med.administrator", AtomicInteger(0))!!
    private val antallGjennomforingerMedOpphavArenaGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.tiltaksgjennomforinger.fra.arena", AtomicInteger(0))!!
    private val antallGjennomforingerMedOpphavAdminflateGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.tiltaksgjennomforinger.fra.adminflate", AtomicInteger(0))!!
    private val antallDeltakereMedOpphavAmt: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.deltakere.fra.komet", AtomicInteger(0))!!
    private val antallDeltakereMedOpphavArena: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.deltakere.fra.arena", AtomicInteger(0))!!
    private val antallAvtalerFraAdminFlateGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.fra.adminflate", AtomicInteger(0))!!
    private val antallAvtalerFraArenaGauge: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.fra.arena", AtomicInteger(0))!!
    private val antallAvtaleNotater: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.avtaler.notater", AtomicInteger(0))!!
    private val antallTiltaksgjennomforingNotater: AtomicInteger =
        Metrikker.appMicrometerRegistry.gauge("antall.tiltaksgjennomforinger.notater", AtomicInteger(0))!!

    fun oppdaterMetrikker() {
        antallUlesteGauge.set(metrikkRepository.hentAntallUlesteNotifikasjoner())
        antallLesteGauge.set(metrikkRepository.hentAntallLesteNotifikasjoner())
        antallAvtalerMedAdministratorGauge.set(metrikkRepository.hentAntallAvtalerMedAdministrator())
        antallGjennomforingerMedAdministratorGauge.set(metrikkRepository.hentAntallTiltaksgjennomforingerMedAdministrator())
        antallDeltakereMedOpphavAmt.set(metrikkRepository.hentAntallDeltakerMedOpphav(opphav = Deltakeropphav.AMT))
        antallDeltakereMedOpphavArena.set(metrikkRepository.hentAntallDeltakerMedOpphav(opphav = Deltakeropphav.ARENA))
        antallAvtalerFraAdminFlateGauge.set(metrikkRepository.hentAntallAvtalerMedOpphav(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE))
        antallAvtalerFraArenaGauge.set(metrikkRepository.hentAntallAvtalerMedOpphav(opphav = ArenaMigrering.Opphav.ARENA))
        antallGjennomforingerMedOpphavArenaGauge.set(metrikkRepository.hentAntallTiltaksgjennomforingerMedOpphav(opphav = ArenaMigrering.Opphav.ARENA))
        antallGjennomforingerMedOpphavAdminflateGauge.set(
            metrikkRepository.hentAntallTiltaksgjennomforingerMedOpphav(
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
            ),
        )
        antallAvtaleNotater.set(metrikkRepository.hentAntallAvtaleNotater())
        antallTiltaksgjennomforingNotater.set(metrikkRepository.hentAntallTiltaksgjennomforingNotater())
    }
}
