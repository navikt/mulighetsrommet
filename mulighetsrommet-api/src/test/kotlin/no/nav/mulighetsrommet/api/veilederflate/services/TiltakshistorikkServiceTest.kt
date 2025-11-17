package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.Either
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelseFraKomet
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserResponse
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.models.*
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentHistoriskeIdenterPdlQuery
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggleContext
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.tiltak.historikk.TiltakshistorikkClient
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto.Arrangor
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto.Gjennomforing
import no.nav.tiltak.historikk.TiltakshistorikkV1Response
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val gjennomforing = GjennomforingFixtures.Oppfolging1

    val tiltakshistorikkOppfolging = TiltakshistorikkV1Dto.GruppetiltakDeltakelse(
        id = UUID.randomUUID(),
        tiltakstype = TiltakshistorikkV1Dto.GruppetiltakDeltakelse.Tiltakstype(
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
            navn = null,
        ),
        gjennomforing = Gjennomforing(
            id = gjennomforing.id,
            navn = gjennomforing.navn,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        ),
        norskIdent = NorskIdent("12345678910"),
        status = DeltakerStatus(
            type = DeltakerStatusType.VENTELISTE,
            opprettetDato = LocalDateTime.of(2018, 12, 3, 0, 0),
            aarsak = null,
        ),
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arrangor = Arrangor(ArrangorFixtures.underenhet1.organisasjonsnummer),
    )

    val tiltakshistorikkAvklaring = TiltakshistorikkV1Dto.ArenaDeltakelse(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.VENTELISTE,
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
            tiltakskode = TiltakstypeFixtures.Avklaring.arenaKode,
            navn = null,
        ),
        arenaTiltakskode = TiltakstypeFixtures.Avklaring.arenaKode,
        beskrivelse = "Avklaring",
        arrangor = Arrangor(Organisasjonsnummer("123456789")),
    )

    val tiltakshistorikkArbeidstrening = TiltakshistorikkV1Dto.ArbeidsgiverAvtale(
        norskIdent = NorskIdent("12345678910"),
        startDato = LocalDate.of(2020, 1, 1),
        sluttDato = LocalDate.of(2021, 12, 31),
        id = UUID.randomUUID(),
        tiltakskode = TiltakshistorikkV1Dto.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING,
        tiltakstype = TiltakshistorikkV1Dto.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING,
        status = ArbeidsgiverAvtaleStatus.GJENNOMFORES,
        arbeidsgiver = TiltakshistorikkV1Dto.Arbeidsgiver(ArrangorFixtures.underenhet2.organisasjonsnummer.value),
    )

    val deltakelseOppfolgingFraKomet = DeltakelseFraKomet(
        deltakerId = tiltakshistorikkOppfolging.id,
        deltakerlisteId = tiltakshistorikkOppfolging.gjennomforing.id,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstype = DeltakelserResponse.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            tiltakskode = "INDOPPFAG",
        ),
        status = DeltakelseFraKomet.Status(
            type = DeltakerStatusType.VENTELISTE,
            visningstekst = "Venteliste",
            aarsak = null,
        ),
        periode = DeltakelseFraKomet.Periode(
            startdato = LocalDate.of(2019, 1, 1),
            sluttdato = LocalDate.of(2019, 12, 3),
        ),
        innsoktDato = LocalDate.of(2018, 12, 3),
        sistEndretDato = LocalDate.of(2018, 12, 5),
    )

    val deltakelseOppfolging = Deltakelse(
        id = tiltakshistorikkOppfolging.id,
        eierskap = DeltakelseEierskap.TEAM_KOMET,
        tilstand = DeltakelseTilstand.AKTIV,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstype = DeltakelseTiltakstype(TiltakstypeFixtures.Oppfolging.navn, Tiltakskode.OPPFOLGING),
        status = DeltakelseStatus(
            type = DataElement.Status("Venteliste", DataElement.Status.Variant.ALT_1),
            aarsak = null,
        ),
        periode = DeltakelsePeriode(
            startDato = LocalDate.of(2019, 1, 1),
            sluttDato = LocalDate.of(2019, 12, 3),
        ),
        sistEndretDato = LocalDate.of(2018, 12, 5),
        innsoktDato = LocalDate.of(2018, 12, 3),
        pamelding = DeltakelsePamelding(
            gjennomforingId = tiltakshistorikkOppfolging.gjennomforing.id,
            status = DeltakerStatusType.VENTELISTE,
        ),
    )
    val deltakelseAvklaring = Deltakelse(
        id = tiltakshistorikkAvklaring.id,
        eierskap = DeltakelseEierskap.ARENA,
        tilstand = DeltakelseTilstand.AKTIV,
        tittel = "Avklaring hos Hovedenhet AS",
        tiltakstype = DeltakelseTiltakstype(TiltakstypeFixtures.Avklaring.navn, Tiltakskode.AVKLARING),
        status = DeltakelseStatus(
            type = DataElement.Status("Venteliste", DataElement.Status.Variant.ALT_1),
            aarsak = null,
        ),
        periode = DeltakelsePeriode(
            startDato = LocalDate.of(2018, 12, 3),
            sluttDato = LocalDate.of(2019, 12, 3),
        ),
        sistEndretDato = null,
        innsoktDato = null,
        pamelding = null,
    )
    val deltakelseArbeidstrening = Deltakelse(
        id = tiltakshistorikkArbeidstrening.id,
        eierskap = DeltakelseEierskap.TEAM_TILTAK,
        tilstand = DeltakelseTilstand.AKTIV,
        tittel = "Arbeidstrening hos Underenhet 2 AS",
        tiltakstype = DeltakelseTiltakstype("Arbeidstrening", null),
        status = DeltakelseStatus(
            type = DataElement.Status("Gjennomføres", DataElement.Status.Variant.BLANK),
            aarsak = null,
        ),
        periode = DeltakelsePeriode(
            startDato = LocalDate.of(2020, 1, 1),
            sluttDato = LocalDate.of(2021, 12, 31),
        ),
        sistEndretDato = null,
        innsoktDato = null,
        pamelding = null,
    )

    val historiskeIdenterQuery: HentHistoriskeIdenterPdlQuery = mockk()
    val tiltakshistorikkClient: TiltakshistorikkClient = mockk()
    val amtDeltakerClient: AmtDeltakerClient = mockk()

    fun createTiltakshistorikkService(isEnabled: () -> Boolean = { false }) = TiltakshistorikkService(
        historiskeIdenterQuery = historiskeIdenterQuery,
        tiltakstypeService = TiltakstypeService(database.db),
        amtDeltakerClient = amtDeltakerClient,
        tiltakshistorikkClient = tiltakshistorikkClient,
        arrangorService = ArrangorService(database.db, mockk()),
        features = object : FeatureToggleService {
            override fun isEnabled(feature: FeatureToggle, context: FeatureToggleContext) = isEnabled()
            override fun isEnabledForTiltakstype(feature: FeatureToggle, vararg tiltakskoder: Tiltakskode) = isEnabled()
        },
    )

    coEvery { historiskeIdenterQuery.hentHistoriskeIdenter(any(), any()) } returns listOf(
        IdentInformasjon(
            ident = PdlIdent("12345678910"),
            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
            historisk = false,
        ),
    ).right()

    beforeAny {
        MulighetsrommetTestDomain(
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(
                TiltakstypeFixtures.Oppfolging,
                TiltakstypeFixtures.Avklaring,
                TiltakstypeFixtures.Arbeidstrening,
                TiltakstypeFixtures.EnkelAmo,
            ),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(gjennomforing),
        ).initialize(database.db)
    }

    test("henter historikk for bruker basert på person id") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkOppfolging, tiltakshistorikkAvklaring, tiltakshistorikkArbeidstrening),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(deltakelseOppfolgingFraKomet),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(deltakelseArbeidstrening, deltakelseOppfolging, deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    test("inkluderer deltakelser fra komet når de ikke finnes i tiltakshistorikken") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkAvklaring),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(deltakelseOppfolgingFraKomet),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(deltakelseOppfolging, deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    test("ikke inkluder informasjon om påmelding når deltakelse er avsluttet") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkAvklaring),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(
                    deltakelseOppfolgingFraKomet.copy(
                        status = DeltakelseFraKomet.Status(
                            type = DeltakerStatusType.HAR_SLUTTET,
                            visningstekst = "Har sluttet",
                            aarsak = null,
                        ),
                    ),
                ),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(
                deltakelseOppfolging.copy(
                    tilstand = DeltakelseTilstand.AVSLUTTET,
                    status = DeltakelseStatus(
                        type = DataElement.Status("Har sluttet", DataElement.Status.Variant.ALT_1),
                        aarsak = null,
                    ),
                    pamelding = null,
                ),
                deltakelseAvklaring,
            ),
            historiske = emptyList(),
        )
    }

    test("viser kun deltakelser fra tiltakshistorikken når det ikke returneres deltakelser fra komet") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkAvklaring),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    test("sorterer deltakelser basert nyeste startdato") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkAvklaring, tiltakshistorikkOppfolging),
            meldinger = setOf(),
        )

        val deltakelseOppfolgingUtenStartdato = deltakelseOppfolgingFraKomet.copy(
            deltakerId = UUID.randomUUID(),
            status = DeltakelseFraKomet.Status(type = DeltakerStatusType.KLADD, visningstekst = "Kladd"),
            periode = null,
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(deltakelseOppfolgingFraKomet, deltakelseOppfolgingUtenStartdato),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        val expectedDeltakelseUtenStartdato = deltakelseOppfolging.copy(
            id = deltakelseOppfolgingUtenStartdato.deltakerId,
            periode = DeltakelsePeriode(null, null),
            tilstand = DeltakelseTilstand.KLADD,
            status = DeltakelseStatus(
                type = DataElement.Status("Kladd", DataElement.Status.Variant.WARNING),
                aarsak = null,
            ),
            pamelding = DeltakelsePamelding(
                gjennomforingId = tiltakshistorikkOppfolging.gjennomforing.id,
                status = DeltakerStatusType.KLADD,
            ),
        )
        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(expectedDeltakelseUtenStartdato, deltakelseOppfolging, deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    context("enkeltplasser fra Komet") {
        val tiltakshistorikkEnkelAmo = TiltakshistorikkV1Dto.ArenaDeltakelse(
            id = UUID.randomUUID(),
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.VENTELISTE,
            startDato = LocalDate.of(2018, 12, 3),
            sluttDato = LocalDate.of(2019, 12, 3),
            arenaTiltakskode = TiltakstypeFixtures.EnkelAmo.arenaKode,
            tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.EnkelAmo.arenaKode,
                navn = null,
            ),
            beskrivelse = "Tilfeldig enkeltplass fra Arena",
            arrangor = Arrangor(Organisasjonsnummer("123456789")),
        )

        val deltakelseEnkelAmo = DeltakelseFraKomet(
            deltakerId = UUID.randomUUID(),
            deltakerlisteId = UUID.randomUUID(),
            tittel = "Tilfeldig enkeltplass fra Komet",
            tiltakstype = DeltakelserResponse.Tiltakstype(
                navn = TiltakstypeFixtures.EnkelAmo.navn,
                tiltakskode = TiltakstypeFixtures.EnkelAmo.arenaKode,
            ),
            status = DeltakelseFraKomet.Status(
                type = DeltakerStatusType.VENTELISTE,
                visningstekst = "Venteliste",
                aarsak = null,
            ),
            periode = DeltakelseFraKomet.Periode(
                startdato = LocalDate.of(2019, 1, 1),
                sluttdato = LocalDate.of(2019, 12, 3),
            ),
            innsoktDato = LocalDate.of(2018, 12, 3),
            sistEndretDato = LocalDate.of(2018, 12, 5),
        )

        test("viser enkeltplasser fra Arena når feature toggle for enkeltplasser er deaktivert") {
            coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
                historikk = listOf(tiltakshistorikkEnkelAmo),
                meldinger = setOf(),
            )

            coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
                DeltakelserResponse(
                    aktive = listOf(deltakelseEnkelAmo),
                    historikk = emptyList(),
                ),
            )

            val isEnkeltplasserFraKometEnabled = { false }

            val historikkService = createTiltakshistorikkService(isEnkeltplasserFraKometEnabled)

            val historikk = historikkService.hentHistorikk(
                NorskIdent("12345678910"),
                AccessType.OBO("token"),
            )

            historikk shouldBe Deltakelser(
                meldinger = setOf(),
                aktive = listOf(
                    Deltakelse(
                        id = tiltakshistorikkEnkelAmo.id,
                        eierskap = DeltakelseEierskap.ARENA,
                        tilstand = DeltakelseTilstand.AKTIV,
                        tittel = "Enkel AMO hos Hovedenhet AS",
                        tiltakstype = DeltakelseTiltakstype(
                            TiltakstypeFixtures.EnkelAmo.navn,
                            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                        ),
                        status = DeltakelseStatus(
                            type = DataElement.Status("Venteliste", DataElement.Status.Variant.ALT_1),
                            aarsak = null,
                        ),
                        periode = DeltakelsePeriode(
                            startDato = LocalDate.of(2018, 12, 3),
                            sluttDato = LocalDate.of(2019, 12, 3),
                        ),
                        sistEndretDato = null,
                        innsoktDato = null,
                        pamelding = null,
                    ),
                ),
                historiske = emptyList(),
            )
        }

        test("viser enkeltplasser fra komet når feature toggle for enkeltplasser er aktivert") {
            coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkV1Response(
                historikk = listOf(tiltakshistorikkEnkelAmo),
                meldinger = setOf(),
            )

            coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
                DeltakelserResponse(
                    aktive = listOf(deltakelseEnkelAmo),
                    historikk = emptyList(),
                ),
            )

            val isEnkeltplasserFraKometEnabled = { true }

            val historikkService = createTiltakshistorikkService(isEnkeltplasserFraKometEnabled)

            val historikk = historikkService.hentHistorikk(
                NorskIdent("12345678910"),
                AccessType.OBO("token"),
            )

            historikk shouldBe Deltakelser(
                meldinger = setOf(),
                aktive = listOf(
                    Deltakelse(
                        id = deltakelseEnkelAmo.deltakerId,
                        eierskap = DeltakelseEierskap.TEAM_KOMET,
                        tilstand = DeltakelseTilstand.AKTIV,
                        tittel = "Tilfeldig enkeltplass fra Komet",
                        tiltakstype = DeltakelseTiltakstype(
                            TiltakstypeFixtures.EnkelAmo.navn,
                            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                        ),
                        status = DeltakelseStatus(
                            type = DataElement.Status("Venteliste", DataElement.Status.Variant.ALT_1),
                            aarsak = null,
                        ),
                        periode = DeltakelsePeriode(
                            startDato = LocalDate.of(2019, 1, 1),
                            sluttDato = LocalDate.of(2019, 12, 3),
                        ),
                        sistEndretDato = LocalDate.of(2018, 12, 5),
                        innsoktDato = LocalDate.of(2018, 12, 3),
                        pamelding = null,
                    ),
                ),
                historiske = emptyList(),
            )
        }
    }
})
