package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.Either
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelseFraKomet
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserResponse
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseEierskap
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelsePamelding
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelsePeriode
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseStatus
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseTilstand
import no.nav.mulighetsrommet.api.veilederflate.models.DeltakelseTiltakstype
import no.nav.mulighetsrommet.api.veilederflate.pdl.HentHistoriskeIdenterPdlQuery
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.service.FeatureToggleService
import no.nav.mulighetsrommet.model.ArbeidsgiverAvtaleStatus
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.tiltak.historikk.TiltakshistorikkClient
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto.Arrangor
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto.Gjennomforing
import no.nav.tiltak.historikk.TiltakshistorikkV1Response
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltakshistorikkServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val gjennomforing = GjennomforingFixtures.Oppfolging1

    val migrertConfig = mapOf(
        Tiltakskode.AVKLARING to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.JOBBKLUBB to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.ARBEIDSRETTET_REHABILITERING to setOf(TiltakstypeFeature.MIGRERT),
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET to setOf(TiltakstypeFeature.MIGRERT),
    )

    val tiltakshistorikkOppfolging = TiltakshistorikkV1Dto.TeamKometDeltakelse(
        id = UUID.randomUUID(),
        tiltakstype = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
            navn = TiltakstypeFixtures.Oppfolging.navn,
        ),
        gjennomforing = Gjennomforing(
            id = gjennomforing.id,
            navn = gjennomforing.navn,
            deltidsprosent = 100f,
        ),
        norskIdent = NorskIdent("12345678910"),
        status = DeltakerStatus(
            type = DeltakerStatusType.VENTELISTE,
            opprettetDato = LocalDateTime.of(2018, 12, 3, 0, 0),
            aarsak = null,
        ),
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        tittel = "Oppfølging hos Hovedenhet AS",
        arrangor = Arrangor(
            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("123456789"), "Hovedenhet AS"),
            underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("976663934"), "Underenhet 1 AS"),
        ),
        deltidsprosent = 100f,
        dagerPerUke = 5f,
    )

    val tiltakshistorikkIps = TiltakshistorikkV1Dto.ArenaDeltakelse(
        id = UUID.randomUUID(),
        arenaId = 1,
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.VENTELISTE,
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
            tiltakskode = "IPSUNG",
            navn = "IPS (Individuell jobbstøtte)",
        ),
        gjennomforing = Gjennomforing(
            id = UUID.randomUUID(),
            navn = "IPS",
            deltidsprosent = 100f,
        ),
        tittel = "IPS (Individuell jobbstøtte) hos Underenhet 1 AS",
        arrangor = Arrangor(
            hovedenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("123456789"), "Hovedenhet AS"),
            underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("976663934"), "Underenhet 1 AS"),
        ),
        deltidsprosent = 100f,
        dagerPerUke = 5f,
    )

    val tiltakshistorikkArbeidstrening = TiltakshistorikkV1Dto.TeamTiltakAvtale(
        norskIdent = NorskIdent("12345678910"),
        startDato = LocalDate.of(2020, 1, 1),
        sluttDato = LocalDate.of(2021, 12, 31),
        id = UUID.randomUUID(),
        tiltakstype = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype(
            tiltakskode = TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakskode.ARBEIDSTRENING,
            navn = "Arbeidstrening",
        ),
        status = ArbeidsgiverAvtaleStatus.GJENNOMFORES,
        tittel = "Arbeidstrening hos Underenhet 2 AS",
        stillingsprosent = 100f,
        dagerPerUke = 5f,
        arbeidsgiver = TiltakshistorikkV1Dto.Virksomhet(
            organisasjonsnummer = ArrangorFixtures.underenhet2.organisasjonsnummer,
            navn = "Underenhet 2 AS",
        ),
    )

    val deltakelseOppfolgingFraKomet = DeltakelseFraKomet(
        deltakerId = tiltakshistorikkOppfolging.id,
        deltakerlisteId = tiltakshistorikkOppfolging.gjennomforing.id,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstype = DeltakelserResponse.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            tiltakskode = Tiltakskode.OPPFOLGING,
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
    val deltakelseIps = Deltakelse(
        id = tiltakshistorikkIps.id,
        eierskap = DeltakelseEierskap.ARENA,
        tilstand = DeltakelseTilstand.AKTIV,
        tittel = "IPS (Individuell jobbstøtte) hos Underenhet 1 AS",
        tiltakstype = DeltakelseTiltakstype("IPS (Individuell jobbstøtte)", null),
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
        tiltakstypeService = TiltakstypeService(TiltakstypeService.Config(migrertConfig), database.db),
        amtDeltakerClient = amtDeltakerClient,
        tiltakshistorikkClient = tiltakshistorikkClient,
        features = object : FeatureToggleService {
            override fun isEnabled(feature: FeatureToggle) = isEnabled()
        },
    )

    coEvery { historiskeIdenterQuery.hentHistoriskeIdenter(any(), any()) } returns listOf(
        IdentInformasjon(
            ident = PdlIdent("12345678910"),
            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
            historisk = false,
        ),
    ).right()

    beforeSpec {
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
        coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkOppfolging, tiltakshistorikkIps, tiltakshistorikkArbeidstrening),
            meldinger = setOf(),
        ).right()

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
            aktive = listOf(deltakelseArbeidstrening, deltakelseOppfolging, deltakelseIps),
            historiske = emptyList(),
        )
    }

    test("inkluderer deltakelser fra komet når de ikke finnes i tiltakshistorikken") {
        coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkIps),
            meldinger = setOf(),
        ).right()

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
            aktive = listOf(deltakelseOppfolging, deltakelseIps),
            historiske = emptyList(),
        )
    }

    test("ikke inkluder informasjon om påmelding når deltakelse er avsluttet") {
        coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkIps),
            meldinger = setOf(),
        ).right()

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
                deltakelseIps,
            ),
            historiske = emptyList(),
        )
    }

    test("viser kun deltakelser fra tiltakshistorikken når det ikke returneres deltakelser fra komet") {
        coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkIps),
            meldinger = setOf(),
        ).right()

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
            aktive = listOf(deltakelseIps),
            historiske = emptyList(),
        )
    }

    test("sorterer deltakelser basert nyeste startdato") {
        coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
            historikk = listOf(tiltakshistorikkIps, tiltakshistorikkOppfolging),
            meldinger = setOf(),
        ).right()

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
            aktive = listOf(expectedDeltakelseUtenStartdato, deltakelseOppfolging, deltakelseIps),
            historiske = emptyList(),
        )
    }

    context("enkeltplasser fra Komet") {
        val tiltakshistorikkEnkelAmo = TiltakshistorikkV1Dto.ArenaDeltakelse(
            id = UUID.randomUUID(),
            arenaId = 1,
            norskIdent = NorskIdent("12345678910"),
            status = ArenaDeltakerStatus.VENTELISTE,
            startDato = LocalDate.of(2018, 12, 3),
            sluttDato = LocalDate.of(2019, 12, 3),
            tittel = "Enkel AMO hos Underenhet 1 AS",
            tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
                tiltakskode = TiltakstypeFixtures.EnkelAmo.arenaKode,
                navn = TiltakstypeFixtures.EnkelAmo.navn,
            ),
            gjennomforing = Gjennomforing(
                id = UUID.randomUUID(),
                navn = "Tilfeldig enkeltplass fra Arena",
                deltidsprosent = 100f,
            ),
            arrangor = Arrangor(
                hovedenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("123456789"), "Hovedenhet AS"),
                underenhet = TiltakshistorikkV1Dto.Virksomhet(Organisasjonsnummer("976663934"), "Underenhet 1 AS"),
            ),
            deltidsprosent = 100f,
            dagerPerUke = 5f,
        )

        val deltakelseEnkelAmo = DeltakelseFraKomet(
            deltakerId = UUID.randomUUID(),
            deltakerlisteId = UUID.randomUUID(),
            tittel = "Tilfeldig enkeltplass fra Komet",
            tiltakstype = DeltakelserResponse.Tiltakstype(
                navn = TiltakstypeFixtures.EnkelAmo.navn,
                tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
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
            coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
                historikk = listOf(tiltakshistorikkEnkelAmo),
                meldinger = setOf(),
            ).right()

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
                        tittel = "Enkel AMO hos Underenhet 1 AS",
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
            coEvery { tiltakshistorikkClient.getHistorikk(any()) } returns TiltakshistorikkV1Response(
                historikk = listOf(tiltakshistorikkEnkelAmo),
                meldinger = setOf(),
            ).right()

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
