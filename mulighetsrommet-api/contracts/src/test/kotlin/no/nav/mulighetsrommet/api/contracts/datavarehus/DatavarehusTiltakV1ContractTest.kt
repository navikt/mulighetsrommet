package no.nav.mulighetsrommet.api.contracts.datavarehus

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.contracts.test.assertContract
import no.nav.mulighetsrommet.api.domain.gjennomforing.Artskontokode
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import no.nav.mulighetsrommet.api.domain.opplaring.ForerkortKlasse
import no.nav.mulighetsrommet.api.domain.opplaring.InnholdElement
import no.nav.mulighetsrommet.api.domain.opplaring.Kurstype
import no.nav.mulighetsrommet.api.domain.opplaring.Sertifisering
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DatavarehusTiltakV1ContractTest : FunSpec({
    val avtale = DatavarehusTiltakV1.Avtale(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        navn = "Opplæringsavtale",
        opprettetTidspunkt = LocalDateTime.parse("2026-01-02T10:15:30"),
        oppdatertTidspunkt = LocalDateTime.parse("2026-02-03T11:16:31.123456"),
    )
    val gjennomforing = DatavarehusTiltakV1.Gjennomforing(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        opprettetTidspunkt = LocalDateTime.parse("2026-01-02T10:15:30"),
        oppdatertTidspunkt = LocalDateTime.parse("2026-02-03T11:16:31.123456"),
        arrangor = DatavarehusTiltakV1.Arrangor(Organisasjonsnummer("999999999")),
        navn = "Opplæring",
        oppstartstype = GjennomforingOppstartstype.FELLES,
        pameldingstype = GjennomforingPameldingType.TRENGER_GODKJENNING,
        startDato = LocalDate.of(2026, 3, 1),
        sluttDato = LocalDate.of(2026, 6, 30),
        status = GjennomforingStatusType.GJENNOMFORES,
        arena = DatavarehusTiltakV1.ArenaData(aar = 2026, lopenummer = 123),
        deltidsprosent = 80.5,
    )

    test("tiltak med alle felter") {
        assertContract<DatavarehusTiltakV1>(
            DatavarehusTiltakV1Dto(
                tiltakskode = Tiltakskode.OPPFOLGING,
                artskontokode = Artskontokode.OPPFOLGING,
                avtale = avtale,
                gjennomforing = gjennomforing,
            ),
        )
    }

    test("tiltak med nullable felter") {
        assertContract<DatavarehusTiltakV1>(
            DatavarehusTiltakV1Dto(
                tiltakskode = Tiltakskode.ARBEIDSTRENING,
                artskontokode = null,
                avtale = null,
                gjennomforing = gjennomforing.copy(
                    navn = null,
                    startDato = null,
                    sluttDato = null,
                    status = null,
                    arena = null,
                    deltidsprosent = null,
                ),
            ),
        )
    }

    test("amo med kategorisering") {
        assertContract<DatavarehusTiltakV1>(
            DatavarehusTiltakV1AmoDto(
                tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
                artskontokode = Artskontokode.ARBEIDSMARKEDSOPPLAERING,
                avtale = avtale,
                gjennomforing = gjennomforing,
                amoKategorisering = DvhAmoKategorisering(
                    kurstype = Kurstype.Kode.BRANSJE_OG_YRKESRETTET,
                    bransje = Bransje.Kode.REISELIV_SERVERING_OG_TRANSPORT,
                    forerkort = listOf(ForerkortKlasse.Kode.C, ForerkortKlasse.Kode.CE),
                    innholdElementer = listOf(InnholdElement.Kode.TEORETISK_OPPLAERING, InnholdElement.Kode.PRAKSIS),
                    norskprove = false,
                    sertifiseringer = listOf(Sertifisering(konseptId = 123L, label = "Truckførerbevis")),
                ),
            ),
        )
    }

    test("amo uten kategorisering") {
        assertContract<DatavarehusTiltakV1>(
            DatavarehusTiltakV1AmoDto(
                tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
                artskontokode = Artskontokode.ARBEIDSMARKEDSOPPLAERING,
                avtale = avtale,
                gjennomforing = gjennomforing,
                amoKategorisering = null,
            ),
        )
    }

    test("amo med standardverdier i kategorisering") {
        assertContract(DvhAmoKategorisering())
    }

    test("yrkesfag med utdanningsløp") {
        assertContract<DatavarehusTiltakV1>(
            DatavarehusTiltakV1YrkesfagDto(
                tiltakskode = Tiltakskode.FAG_OG_YRKESOPPLAERING,
                artskontokode = Artskontokode.FAG_OG_YRKESOPPLAERING,
                avtale = avtale,
                gjennomforing = gjennomforing,
                utdanningslop = DvhUtdanningslop(
                    utdanningsprogram = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                    utdanninger = setOf(
                        UUID.fromString("00000000-0000-0000-0000-000000000004"),
                        UUID.fromString("00000000-0000-0000-0000-000000000005"),
                    ),
                ),
            ),
        )
    }

    test("yrkesfag uten utdanningsløp") {
        assertContract<DatavarehusTiltakV1>(
            DatavarehusTiltakV1YrkesfagDto(
                tiltakskode = Tiltakskode.FAG_OG_YRKESOPPLAERING,
                artskontokode = Artskontokode.FAG_OG_YRKESOPPLAERING,
                avtale = avtale,
                gjennomforing = gjennomforing,
                utdanningslop = null,
            ),
        )
    }
})
