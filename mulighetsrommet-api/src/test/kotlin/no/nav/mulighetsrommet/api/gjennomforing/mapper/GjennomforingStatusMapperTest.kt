package no.nav.mulighetsrommet.api.gjennomforing.mapper

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.model.GjennomforingStatus
import java.time.LocalDate
import java.time.LocalDateTime

class GjennomforingStatusMapperTest : FunSpec({

    test("status blir AVSLUTTET når sluttdato er etter dagens dato") {
        val today = LocalDate.of(2024, 1, 1)
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        GjennomforingStatusMapper.fromSluttDato(
            sluttDato = null,
            today = today,
        ) shouldBe GjennomforingStatus.GJENNOMFORES

        GjennomforingStatusMapper.fromSluttDato(
            sluttDato = today,
            today = today,
        ) shouldBe GjennomforingStatus.GJENNOMFORES

        GjennomforingStatusMapper.fromSluttDato(
            sluttDato = tomorrow,
            today = today,
        ) shouldBe GjennomforingStatus.GJENNOMFORES

        GjennomforingStatusMapper.fromSluttDato(
            sluttDato = yesterday,
            today = today,
        ) shouldBe GjennomforingStatus.AVSLUTTET
    }

    test("status blir satt basert avsluttet tidspunkt i forhold start- og sluttdato") {
        val startDato = LocalDate.of(2024, 2, 1)
        val sluttDato = LocalDate.of(2024, 2, 10)

        GjennomforingStatusMapper.fromAvsluttetTidspunkt(
            startDato = startDato,
            sluttDato = sluttDato,
            avsluttetTidspunkt = LocalDateTime.of(2024, 1, 31, 0, 0),
        ) shouldBe GjennomforingStatus.AVLYST

        GjennomforingStatusMapper.fromAvsluttetTidspunkt(
            startDato = startDato,
            sluttDato = sluttDato,
            avsluttetTidspunkt = LocalDateTime.of(2024, 2, 1, 0, 0),
        ) shouldBe GjennomforingStatus.AVBRUTT

        GjennomforingStatusMapper.fromAvsluttetTidspunkt(
            startDato = startDato,
            sluttDato = sluttDato,
            avsluttetTidspunkt = LocalDateTime.of(2024, 2, 10, 0, 0),
        ) shouldBe GjennomforingStatus.AVBRUTT

        GjennomforingStatusMapper.fromAvsluttetTidspunkt(
            startDato = startDato,
            sluttDato = sluttDato,
            avsluttetTidspunkt = LocalDateTime.of(2024, 2, 11, 0, 0),
        ) shouldBe GjennomforingStatus.AVSLUTTET
    }
})
