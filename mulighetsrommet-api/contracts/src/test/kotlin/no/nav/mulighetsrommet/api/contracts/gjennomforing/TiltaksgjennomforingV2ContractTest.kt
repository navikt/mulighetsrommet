package no.nav.mulighetsrommet.api.contracts.gjennomforing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto.EnkeltplassPrisinformasjon
import no.nav.mulighetsrommet.api.contracts.test.assertContract
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TiltaksgjennomforingV2ContractTest : FunSpec({
    val gruppe = TiltaksgjennomforingV2Dto.Gruppe(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        lopenummer = Tiltaksnummer("2026/123"),
        opprettetTidspunkt = Instant.parse("2026-01-02T10:15:30Z"),
        oppdatertTidspunkt = Instant.parse("2026-02-03T11:16:31.123456Z"),
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("999999999")),
        status = TiltaksgjennomforingV2Dto.Status.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
        navn = "Norskopplæring",
        startDato = LocalDate.of(2026, 3, 1),
        sluttDato = LocalDate.of(2026, 6, 30),
        tilgjengeligForArrangorFraOgMedDato = LocalDate.of(2026, 2, 15),
        apentForPamelding = true,
        antallPlasser = 15,
        deltidsprosent = 80.5,
        oppmoteSted = "Tromsø",
    )
    val enkeltplass = TiltaksgjennomforingV2Dto.Enkeltplass(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        lopenummer = Tiltaksnummer("2026/456"),
        opprettetTidspunkt = Instant.parse("2026-01-02T10:15:30Z"),
        oppdatertTidspunkt = Instant.parse("2026-02-03T11:16:31.123456Z"),
        tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("999999999")),
        status = TiltaksgjennomforingV2Dto.Status.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
    )

    test("gruppe med alle felter") {
        assertContract<TiltaksgjennomforingV2Dto>(gruppe)
    }

    test("gruppe med nullable felter") {
        assertContract<TiltaksgjennomforingV2Dto>(
            gruppe.copy(sluttDato = null, tilgjengeligForArrangorFraOgMedDato = null, oppmoteSted = null),
        )
    }

    test("enkeltplass med anskaffelse") {
        assertContract<TiltaksgjennomforingV2Dto>(
            enkeltplass.copy(prisinformasjon = EnkeltplassPrisinformasjon.Anskaffelse(pris = 12500)),
        )
    }

    test("enkeltplass med tilskudd") {
        assertContract<TiltaksgjennomforingV2Dto>(
            enkeltplass.copy(
                prisinformasjon = EnkeltplassPrisinformasjon.Tilskudd(
                    tilskudd = mapOf(
                        Opplaeringtilskudd.Kode.SKOLEPENGER to 12500,
                        Opplaeringtilskudd.Kode.STUDIEREISE to 2300,
                        Opplaeringtilskudd.Kode.EKSAMENSGEBYR to 450,
                        Opplaeringtilskudd.Kode.SEMESTERAVGIFT to 600,
                        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD to 8000,
                    ),
                    tilleggsopplysninger = "Inkluderer læremidler",
                ),
            ),
        )
    }

    test("enkeltplass med tilskudd uten tilleggsopplysninger") {
        assertContract<TiltaksgjennomforingV2Dto>(
            enkeltplass.copy(
                prisinformasjon = EnkeltplassPrisinformasjon.Tilskudd(
                    tilskudd = emptyMap(),
                    tilleggsopplysninger = null,
                ),
            ),
        )
    }

    test("enkeltplass uten kostnader") {
        assertContract<TiltaksgjennomforingV2Dto>(
            enkeltplass.copy(
                prisinformasjon = EnkeltplassPrisinformasjon.IngenKostnader(
                    tilleggsopplysninger = "Offentlig finansiert opplæring",
                ),
            ),
        )
    }

    test("enkeltplass uten løpenummer og prisinformasjon") {
        assertContract<TiltaksgjennomforingV2Dto>(enkeltplass.copy(lopenummer = null))
    }

    test("eldre enkeltplassmeldinger uten prisinformasjon kan fortsatt leses") {
        val json = """
            {
              "type": "TiltaksgjennomforingV2.Enkeltplass",
              "id": "00000000-0000-0000-0000-000000000002",
              "lopenummer": "2026/456",
              "opprettetTidspunkt": "2026-01-02T10:15:30Z",
              "oppdatertTidspunkt": "2026-02-03T11:16:31.123456Z",
              "tiltakskode": "ARBEIDSMARKEDSOPPLAERING",
              "arrangor": { "organisasjonsnummer": "999999999" },
              "status": "GJENNOMFORES",
              "oppstart": "ENKELTPLASS",
              "pameldingType": "DIREKTE_VEDTAK"
            }
        """.trimIndent()

        Json.decodeFromString<TiltaksgjennomforingV2Dto>(json) shouldBe enkeltplass
    }
})
