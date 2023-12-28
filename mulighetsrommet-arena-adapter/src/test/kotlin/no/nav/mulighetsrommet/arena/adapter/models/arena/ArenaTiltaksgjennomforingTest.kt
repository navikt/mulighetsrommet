package no.nav.mulighetsrommet.arena.adapter.models.arena

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus

class ArenaTiltaksgjennomforingTest : FunSpec({

    fun createTiltaksgjennomforingJson(antallDeltakere: Number = 5) = Json.parseToJsonElement(
        """{
            "TILTAKGJENNOMFORING_ID": 3780431,
            "LOKALTNAVN": "Testenavn",
            "TILTAKSKODE": "INDOPPFAG",
            "ARBGIV_ID_ARRANGOR": 49612,
            "SAK_ID": 13572352,
            "REG_DATO": "2022-10-10 00:00:00",
            "DATO_FRA": null,
            "DATO_TIL": null,
            "STATUS_TREVERDIKODE_INNSOKNING": "J",
            "ANTALL_DELTAKERE": $antallDeltakere,
            "TILTAKSTATUSKODE": "GJENNOMFOR",
            "AVTALE_ID": "1000",
            "KLOKKETID_FREMMOTE": "12:30",
            "DATO_FREMMOTE": "2022-10-10 00:00:00",
            "TEKST_KURSSTED": "I huset bortenfor huset"
        }""",
    )

    context("decode from JSON") {
        test("should decode event from JSON") {
            Json.decodeFromJsonElement<ArenaTiltaksgjennomforing>(createTiltaksgjennomforingJson()) shouldBe ArenaTiltaksgjennomforing(
                TILTAKGJENNOMFORING_ID = 3780431,
                SAK_ID = 13572352,
                TILTAKSKODE = "INDOPPFAG",
                LOKALTNAVN = "Testenavn",
                REG_DATO = "2022-10-10 00:00:00",
                ARBGIV_ID_ARRANGOR = 49612,
                DATO_FRA = null,
                DATO_TIL = null,
                STATUS_TREVERDIKODE_INNSOKNING = JaNeiStatus.Ja,
                ANTALL_DELTAKERE = 5,
                TILTAKSTATUSKODE = "GJENNOMFOR",
                AVTALE_ID = 1000,
                KLOKKETID_FREMMOTE = "12:30",
                DATO_FREMMOTE = "2022-10-10 00:00:00",
                TEKST_KURSSTED = "I huset bortenfor huset",
                EKSTERN_ID = null,
            )
        }

        test("should decode ANTALL_DELTAKERE to nearest integer") {
            val json = createTiltaksgjennomforingJson(antallDeltakere = 5.5)

            val decoded = Json.decodeFromJsonElement<ArenaTiltaksgjennomforing>(json)

            decoded.ANTALL_DELTAKERE shouldBe 6
        }
    }
})
