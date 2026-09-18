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
import no.nav.tiltak.okonomi.OkonomiPart
import no.nav.tiltak.okonomi.OkonomiSystem
import no.nav.tiltak.okonomi.OpprettBestilling
import no.nav.tiltak.okonomi.Tilskuddstype
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.service.TiltaksokonomiService
import java.time.Instant
import java.time.LocalDate

class TiltaksadministrasjonBestillingConsumerTest : FunSpec({

    val okonomi = mockk<TiltaksokonomiService>()
    val consumer = TiltaksadministrasjonBestillingConsumer(mockk<KafkaConsumerRepository>(relaxed = true), okonomi)

    fun opprettBestillingMelding(
        bestillingsnummer: String,
        okonomiSystem: OkonomiSystem = OkonomiSystem.TILTAKSADMINISTRASJON,
        tiltakskode: Tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    ): OkonomiBestillingMelding.Bestilling = OkonomiBestillingMelding.Bestilling(
        OpprettBestilling(
            bestillingsnummer = Bestillingsnummer(bestillingsnummer),
            okonomiSystem = okonomiSystem,
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = tiltakskode,
            arrangor = OpprettBestilling.Arrangor.Norsk(Organisasjonsnummer("234567891")),
            avtalenummer = null,
            belop = 1000,
            behandletAv = OkonomiPart.System(okonomiSystem),
            behandletTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            besluttetAv = OkonomiPart.System(okonomiSystem),
            besluttetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            kostnadssted = NavEnhetNummer("0400"),
            valuta = Valuta.NOK,
        ),
    )

    fun toJson(melding: OkonomiBestillingMelding) = Json.encodeToJsonElement<OkonomiBestillingMelding>(melding)

    test("avviser melding når bestillingsnummer ikke starter med 'A-'") {
        val melding = opprettBestillingMelding(bestillingsnummer = "E-1-1")

        val exception = shouldThrow<IllegalArgumentException> {
            consumer.consume("E-1-1", toJson(melding))
        }
        exception.message shouldBe "Ugyldig bestillingsnummer=E-1-1 fra Tiltaksadministrasjon, må starte med 'A-'"

        coVerify(exactly = 0) { okonomi.opprettBestilling(any()) }
    }

    test("avviser bestilling-melding når okonomiSystem ikke er TILTAKSADMINISTRASJON") {
        val melding = opprettBestillingMelding(
            bestillingsnummer = "A-1-1",
            okonomiSystem = OkonomiSystem.EKSPERTBISTAND,
        )

        val exception = shouldThrow<IllegalArgumentException> {
            consumer.consume("A-1-1", toJson(melding))
        }
        exception.message shouldBe "Ugyldig system EKSPERTBISTAND fra Tiltaksadministrasjon"

        coVerify(exactly = 0) { okonomi.opprettBestilling(any()) }
    }

    test("avviser bestilling-melding når tiltakskode er EKSPERTBISTAND") {
        val melding = opprettBestillingMelding(
            bestillingsnummer = "A-1-1",
            tiltakskode = Tiltakskode.EKSPERTBISTAND,
        )

        val exception = shouldThrow<IllegalArgumentException> {
            consumer.consume("A-1-1", toJson(melding))
        }
        exception.message shouldBe "Ugyldig tiltakskode EKSPERTBISTAND fra Tiltaksadministrasjon"

        coVerify(exactly = 0) { okonomi.opprettBestilling(any()) }
    }

    test("delegerer gyldig bestilling til TiltaksokonomiService") {
        val melding = opprettBestillingMelding(bestillingsnummer = "A-1-1")

        coEvery { okonomi.opprettBestilling(melding.payload) } returns mockk<Bestilling>().right()

        consumer.consume("A-1-1", toJson(melding))

        coVerify(exactly = 1) { okonomi.opprettBestilling(melding.payload) }
    }
})
