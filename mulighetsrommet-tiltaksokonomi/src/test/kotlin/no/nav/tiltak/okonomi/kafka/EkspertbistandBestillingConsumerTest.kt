package no.nav.tiltak.okonomi.kafka

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.tiltak.okonomi.Bestillingsnummer
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiFagsystem
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.service.TiltaksokonomiService
import java.time.Instant
import java.time.LocalDate

class EkspertbistandBestillingConsumerTest : FunSpec({

    val okonomi = mockk<TiltaksokonomiService>()
    val consumer = EkspertbistandBestillingConsumer(mockk<KafkaConsumerRepository>(relaxed = true), okonomi)

    fun opprettBestillingMelding(
        bestillingsnummer: String,
        tiltakskode: Tiltakskode = Tiltakskode.EKSPERTBISTAND,
    ): OkonomiBestillingMelding.Bestilling = OkonomiBestillingMelding.Bestilling(
        OpprettBestilling(
            bestillingsnummer = Bestillingsnummer(bestillingsnummer),
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = tiltakskode,
            arrangor = OpprettBestilling.Arrangor.Norsk(Organisasjonsnummer("234567891")),
            avtalenummer = null,
            belop = 1000,
            behandletAv = OkonomiPart.Fagsystem(OkonomiFagsystem.EKSPERTBISTAND),
            behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            besluttetAv = OkonomiPart.Fagsystem(OkonomiFagsystem.EKSPERTBISTAND),
            besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            kostnadssted = NavEnhetNummer("0400"),
            valuta = Valuta.NOK,
        ),
    )

    fun toJson(melding: OkonomiBestillingMelding) = Json.encodeToJsonElement<OkonomiBestillingMelding>(melding)

    test("avviser melding når bestillingsnummer ikke starter med 'E-'") {
        val melding = opprettBestillingMelding(bestillingsnummer = "A-1-1")

        val exception = shouldThrow<IllegalArgumentException> {
            consumer.consume("A-1-1", toJson(melding))
        }
        exception.message shouldBe "Ugyldig bestillingsnummer=A-1-1 fra Ekspertbistand"

        coVerify(exactly = 0) { okonomi.opprettBestilling(any(), any()) }
    }

    test("avviser bestilling-melding når tiltakskode ikke er EKSPERTBISTAND") {
        val melding = opprettBestillingMelding(
            bestillingsnummer = "E-1-1",
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        )

        val exception = shouldThrow<IllegalArgumentException> {
            consumer.consume("E-1-1", toJson(melding))
        }
        exception.message shouldBe "Ugyldig tiltakskode ARBEIDSFORBEREDENDE_TRENING fra Ekspertbistand"

        coVerify(exactly = 0) { okonomi.opprettBestilling(any(), any()) }
    }

    test("delegerer gyldig bestilling til TiltaksokonomiService") {
        val melding = opprettBestillingMelding(bestillingsnummer = "E-1-1")

        coEvery { okonomi.opprettBestilling(OkonomiFagsystem.EKSPERTBISTAND, melding.payload) } returns mockk<Bestilling>().right()

        consumer.consume("E-1-1", toJson(melding))

        coVerify(exactly = 1) { okonomi.opprettBestilling(OkonomiFagsystem.EKSPERTBISTAND, melding.payload) }
    }
})
