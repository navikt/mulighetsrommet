package no.nav.mulighetsrommet.api.domain.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.Instant
import java.util.UUID

class DeltakerTest : FunSpec({
    test("tidspunkt avkortes til mikrosekunders presisjon ved opprettelse") {
        val tidspunktMedNanosekunder = Instant.parse("2023-03-01T00:00:00.123456789Z")
        val tidspunktAvkortetTilMikrosekunder = Instant.parse("2023-03-01T00:00:00.123456Z")

        val deltaker = Deltaker.opprett(
            id = UUID.randomUUID(),
            gjennomforingId = UUID.randomUUID(),
            startDato = null,
            sluttDato = null,
            registrertTidspunkt = tidspunktMedNanosekunder,
            endretTidspunkt = tidspunktMedNanosekunder,
            status = DeltakerStatus(
                type = DeltakerStatusType.DELTAR,
                aarsak = null,
                opprettetTidspunkt = tidspunktMedNanosekunder,
            ),
            deltakelsesmengder = listOf(),
            innholdAnnet = null,
            navVeileder = null,
        )

        deltaker.registrertTidspunkt shouldBe tidspunktAvkortetTilMikrosekunder
        deltaker.endretTidspunkt shouldBe tidspunktAvkortetTilMikrosekunder
        deltaker.status.opprettetTidspunkt shouldBe tidspunktAvkortetTilMikrosekunder
    }
})
