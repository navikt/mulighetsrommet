package no.nav.mulighetsrommet.admin.gjennomforing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.personalia.Gradering
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DeltakerDtoTest : FunSpec({
    val dato = LocalDate.of(2026, 10, 20)
    val deltakerId = UUID.randomUUID()
    val gjennomforingId = UUID.randomUUID()

    fun createDeltaker(
        deltakelsesmengder: List<Deltakelsesmengde> = emptyList(),
        startDato: LocalDate = dato.minusMonths(1),
    ) = DeltakerFixtures.createDeltakerMedDeltakelsesmengder(
        id = deltakerId,
        gjennomforingId = gjennomforingId,
        startDato = startDato,
        sluttDato = dato.plusMonths(1),
        endretTidspunkt = Instant.parse("2026-10-20T08:00:00Z"),
        deltakelsesmengder = deltakelsesmengder,
    )

    val personalia = Personalia(
        deltakerId = deltakerId,
        norskIdent = NorskIdent("12345678901"),
        navn = "Test Testesen",
        oppfolgingEnhet = null,
        geografiskEnhet = null,
        region = null,
        gradering = Gradering.UGRADERT,
        avvistGrunn = null,
    )

    fun mengde(gyldigFra: LocalDate, dagerPerUke: Float?) = Deltakelsesmengde(
        gyldigFra = gyldigFra,
        deltakelsesprosent = null,
        dagerPerUke = dagerPerUke,
        opprettetTidspunkt = Instant.parse("2026-10-01T08:00:00Z"),
    )

    test("dager per uke er null når deltakelsesmengder mangler") {
        DeltakerDto.from(createDeltaker(), personalia, null, dato).dagerPerUke shouldBe null
    }

    test("velger siste gyldige deltakelsesmengde uavhengig av rekkefølge") {
        val deltaker = createDeltaker(
            deltakelsesmengder = listOf(
                mengde(dato.minusDays(1), 3f),
                mengde(dato.plusDays(1), 5f),
                mengde(dato.minusWeeks(1), 2f),
            ),
        )

        DeltakerDto.from(deltaker, personalia, null, dato).dagerPerUke shouldBe 3f
    }

    test("deltakelsesmengde er gyldig fra og med angitt dato") {
        val deltaker = createDeltaker(
            deltakelsesmengder = listOf(
                mengde(dato.minusDays(1), 2f),
                mengde(dato, 4f),
                mengde(dato.plusDays(1), 5f),
            ),
        )

        DeltakerDto.from(deltaker, personalia, null, dato.minusDays(1)).dagerPerUke shouldBe 2f
        DeltakerDto.from(deltaker, personalia, null, dato).dagerPerUke shouldBe 4f
        DeltakerDto.from(deltaker, personalia, null, dato.plusDays(1)).dagerPerUke shouldBe 5f
    }

    test("beholder dager per uke når deltaker og eneste deltakelsesmengde starter i fremtiden") {
        val startDato = dato.plusWeeks(1)
        val deltaker = createDeltaker(
            startDato = startDato,
            deltakelsesmengder = listOf(mengde(startDato, 3f)),
        )

        DeltakerDto.from(deltaker, personalia, null, dato).dagerPerUke shouldBe 3f
    }

    test("beholder null når siste gyldige deltakelsesmengde mangler dager per uke") {
        val deltaker = createDeltaker(
            deltakelsesmengder = listOf(
                mengde(dato.minusWeeks(1), 2f),
                mengde(dato, null),
                mengde(dato.plusWeeks(1), 5f),
            ),
        )

        DeltakerDto.from(deltaker, personalia, null, dato).dagerPerUke shouldBe null
    }
})
