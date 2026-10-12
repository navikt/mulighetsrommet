package no.nav.mulighetsrommet.api.utbetaling

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetaling1
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetalingLinje1
import no.nav.mulighetsrommet.api.fixtures.setUtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingLinjeHandling
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingStatusDto
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingTilstandsendringDbo
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinjeStatus
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.util.UUID

class UtbetalingDtoQueryTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val saksbehandler = NavAnsattFixture.DonaldDuck.medRoller(
        setOf(NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_GRUPPETILTAK)),
    )
    val attestant = NavAnsattFixture.MikkeMus.medRoller(
        setOf(NavAnsattRolle.generell(Rolle.OKONOMI_ATTESTANT_GRUPPETILTAK)),
    )
    val enkeltplassSaksbehandler = NavAnsattFixture.DonaldDuck.medRoller(
        setOf(NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)),
    )
    val enkeltplassBeslutter = NavAnsattFixture.MikkeMus.medRoller(
        setOf(
            NavAnsattRolle.kontorspesifikk(
                Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
                setOf(NavEnhetFixtures.Innlandet.enhetsnummer),
            ),
        ),
    )
    val onBehalfOf = mockk<PersonaliaService.OnBehalfOf.NavAnsatt>()
    val personaliaService = mockk<PersonaliaService>()
    fun createQuery() = UtbetalingDtoQuery(database.api, personaliaService)

    beforeEach {
        coEvery { personaliaService.getPersonalia(any<List<UUID>>(), onBehalfOf) } returns emptyList()
    }

    afterEach {
        database.truncateAll()
    }

    test("utbetalingsdetaljer krever lesetilgang") {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            utbetalinger = listOf(utbetaling1),
        ).initialize(database.api)

        createQuery().getDetaljer(utbetaling1.id, NavAnsattFixture.DonaldDuck.navIdent).shouldBeLeft(
            UtbetalingDtoQueryError.ManglerTilgang("Du mangler lesetilgang til utbetalingen"),
        )
    }

    test("utbetalingsdetaljer inneholder status og avbrytelse til beslutning") {
        val totrinnskontroll = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = utbetaling1.id,
            type = TotrinnskontrollType.UTBETALING_AVBRYTELSE,
            behandletAv = NavAnsattFixture.MikkeMus.navIdent,
        )
        MulighetsrommetTestDomain(
            ansatte = listOf(
                NavAnsattFixture.DonaldDuck.medRoller(setOf(NavAnsattRolle.generell(Rolle.OKONOMI_LES))),
                NavAnsattFixture.MikkeMus,
            ),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            utbetalinger = listOf(utbetaling1),
        ) {
            queries.totrinnskontroll.upsert(totrinnskontroll)
            queries.utbetaling.upsert(
                utbetaling1.copy(
                    status = UtbetalingStatusType.TIL_AVBRYTELSE,
                    avbrytelse = UtbetalingTilstandsendringDbo(totrinnskontroll.id, UtbetalingStatusType.GENERERT),
                ),
            )
        }.initialize(database.api)

        val detaljer = createQuery()
            .getDetaljer(utbetaling1.id, NavAnsattFixture.DonaldDuck.navIdent)
            .shouldBeRight()

        detaljer.utbetaling.id shouldBe utbetaling1.id
        detaljer.utbetaling.status shouldBe UtbetalingStatusDto.fromUtbetalingStatus(
            UtbetalingStatusType.TIL_AVBRYTELSE,
            emptySet(),
            totrinnskontroll,
        )
        detaljer.utbetaling.avbrytelse.shouldBeTypeOf<TotrinnskontrollDto.TilBeslutning>()
    }

    test("enkeltplass gir handling SLETT, men ikke SEND_TIL_AVBRYTELSE") {
        val enkeltplassUtbetaling = utbetaling1.copy(
            id = UUID.randomUUID(),
            gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
            status = UtbetalingStatusType.TIL_BEHANDLING,
        )

        MulighetsrommetTestDomain(
            ansatte = listOf(enkeltplassSaksbehandler),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
            utbetalinger = listOf(enkeltplassUtbetaling),
        ).initialize(database.api)

        val detaljer = createQuery()
            .getDetaljer(enkeltplassUtbetaling.id, enkeltplassSaksbehandler.navIdent)
            .shouldBeRight()

        detaljer.handlinger shouldContain UtbetalingHandling.SLETT
        detaljer.handlinger shouldNotContain UtbetalingHandling.SEND_TIL_AVBRYTELSE
    }

    test("avtaleutbetaling til behandling tilbyr handlinger for saksbehandler") {
        MulighetsrommetTestDomain(
            ansatte = listOf(saksbehandler),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_BEHANDLING)),
        ).initialize(database.api)

        val detaljer = createQuery()
            .getDetaljer(utbetaling1.id, saksbehandler.navIdent)
            .shouldBeRight()

        detaljer.handlinger shouldBe setOf(
            UtbetalingHandling.SEND_TIL_GODKJENNING,
            UtbetalingHandling.REDIGER,
            UtbetalingHandling.HENT_GODKJENTE_TILSAGN,
            UtbetalingHandling.OPPRETT_TILSAGN,
            UtbetalingHandling.SEND_TIL_AVBRYTELSE,
        )
    }

    test("lesetilgang gir ikke handlinger for en annen gjennomføringstype") {
        val saksbehandlerMedLesetilgang = saksbehandler.medRoller(
            saksbehandler.roller + NavAnsattRolle.generell(Rolle.OKONOMI_LES),
        )
        MulighetsrommetTestDomain(
            ansatte = listOf(saksbehandlerMedLesetilgang),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
            utbetalinger = listOf(
                utbetaling1.copy(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = UtbetalingStatusType.TIL_BEHANDLING,
                ),
            ),
        ).initialize(database.api)

        val detaljer = createQuery()
            .getDetaljer(utbetaling1.id, saksbehandlerMedLesetilgang.navIdent)
            .shouldBeRight()

        detaljer.handlinger.shouldBeEmpty()
    }

    test("ferdigbehandlet utbetaling tilbyr opprettelse av korreksjon") {
        MulighetsrommetTestDomain(
            ansatte = listOf(saksbehandler),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
            utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
        ).initialize(database.api)

        val detaljer = createQuery()
            .getDetaljer(utbetaling1.id, saksbehandler.navIdent)
            .shouldBeRight()

        detaljer.handlinger shouldBe setOf(
            UtbetalingHandling.OPPRETT_KORREKSJON,
        )
    }

    context("avbrytelse") {
        val annenSaksbehandler = NavAnsattFixture.FetterAnton.medRoller(saksbehandler.roller)
        val avbrytelse = Totrinnskontroll.opprett(
            id = UUID.randomUUID(),
            entityId = utbetaling1.id,
            type = TotrinnskontrollType.UTBETALING_AVBRYTELSE,
            behandletAv = saksbehandler.navIdent,
        )
        val utbetalingTilAvbrytelse = utbetaling1.copy(
            status = UtbetalingStatusType.TIL_AVBRYTELSE,
            avbrytelse = UtbetalingTilstandsendringDbo(avbrytelse.id, UtbetalingStatusType.GENERERT),
        )

        test("en annen saksbehandler kan godkjenne eller avslå avbrytelsen") {
            MulighetsrommetTestDomain(
                ansatte = listOf(saksbehandler, annenSaksbehandler, attestant),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1),
            ) {
                queries.totrinnskontroll.upsert(avbrytelse)
                queries.utbetaling.upsert(utbetalingTilAvbrytelse)
            }.initialize(database.api)

            val query = createQuery()

            val saksbehandlerDetaljer = query.getDetaljer(utbetaling1.id, annenSaksbehandler.navIdent).shouldBeRight()

            saksbehandlerDetaljer.handlinger shouldBe setOf(
                UtbetalingHandling.GODKJENN_AVBRYTELSE,
                UtbetalingHandling.AVSLA_AVBRYTELSE,
            )

            val attestantDetaljer = query.getDetaljer(utbetaling1.id, attestant.navIdent).shouldBeRight()

            attestantDetaljer.handlinger.shouldBeEmpty()
        }

        test("saksbehandler kan avslå, men ikke godkjenne sin egen avbrytelse") {
            MulighetsrommetTestDomain(
                ansatte = listOf(saksbehandler),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1),
            ) {
                queries.totrinnskontroll.upsert(avbrytelse)
                queries.utbetaling.upsert(utbetalingTilAvbrytelse)
            }.initialize(database.api)

            val detaljer = createQuery()
                .getDetaljer(utbetaling1.id, saksbehandler.navIdent)
                .shouldBeRight()

            detaljer.handlinger shouldBe setOf(
                UtbetalingHandling.AVSLA_AVBRYTELSE,
            )
        }

        test("besluttet avbrytelse gir ingen handlinger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(saksbehandler, annenSaksbehandler),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                utbetalinger = listOf(utbetaling1),
            ) {
                queries.totrinnskontroll.upsert(avbrytelse.godkjenn(annenSaksbehandler.navIdent).shouldBeRight())
                queries.utbetaling.upsert(utbetalingTilAvbrytelse.copy(status = UtbetalingStatusType.AVBRUTT))
            }.initialize(database.api)

            val detaljer = createQuery()
                .getDetaljer(utbetaling1.id, saksbehandler.navIdent)
                .shouldBeRight()

            detaljer.handlinger.shouldBeEmpty()
        }
    }

    context("utbetalingslinjer") {
        val enkeltplassTilsagn = TilsagnFixtures.createTilsagn(
            gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
            lopenummer = 1,
        )
        val enkeltplassUtbetaling = utbetaling1.copy(
            gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
            status = UtbetalingStatusType.TIL_GODKJENNING,
        )
        val enkeltplassLinje = utbetalingLinje1.copy(tilsagnId = enkeltplassTilsagn.id)

        test("gruppetiltak-attestant kan godkjenne og returnere linjen") {
            MulighetsrommetTestDomain(
                ansatte = listOf(saksbehandler, attestant),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_GODKJENNING)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setUtbetalingLinjeStatus(
                    utbetalingLinje1,
                    UtbetalingLinjeStatus.TIL_GODKJENNING,
                    behandletAv = saksbehandler.navIdent,
                )
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(utbetaling1.id, attestant.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger shouldBe setOf(
                UtbetalingLinjeHandling.GODKJENN,
                UtbetalingLinjeHandling.RETURNER,
            )
        }

        test("enkeltplass-beslutter ved tilsagnets kostnadssted kan godkjenne og returnere") {
            MulighetsrommetTestDomain(
                ansatte = listOf(enkeltplassSaksbehandler, enkeltplassBeslutter),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
                tilsagn = listOf(enkeltplassTilsagn),
                utbetalinger = listOf(enkeltplassUtbetaling),
                utbetalingLinjer = listOf(enkeltplassLinje),
            ) {
                setUtbetalingLinjeStatus(
                    enkeltplassLinje,
                    UtbetalingLinjeStatus.TIL_GODKJENNING,
                    behandletAv = enkeltplassSaksbehandler.navIdent,
                )
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(enkeltplassUtbetaling.id, enkeltplassBeslutter.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger shouldBe setOf(
                UtbetalingLinjeHandling.GODKJENN,
                UtbetalingLinjeHandling.RETURNER,
            )
        }

        test("gruppetiltak-attestant med lesetilgang får ikke handlinger for enkeltplasslinjer") {
            val attestantMedLesetilgang = attestant.medRoller(
                attestant.roller + NavAnsattRolle.generell(Rolle.OKONOMI_LES),
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(enkeltplassSaksbehandler, attestantMedLesetilgang),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
                tilsagn = listOf(enkeltplassTilsagn),
                utbetalinger = listOf(enkeltplassUtbetaling),
                utbetalingLinjer = listOf(enkeltplassLinje),
            ) {
                setUtbetalingLinjeStatus(enkeltplassLinje, UtbetalingLinjeStatus.TIL_GODKJENNING)
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(enkeltplassUtbetaling.id, attestantMedLesetilgang.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger.shouldBeEmpty()
        }

        test("enkeltplass-beslutter uten tilgang til kostnadsstedet får ikke linjehandlinger") {
            val beslutterVedAnnetKostnadssted = enkeltplassBeslutter.medRoller(
                setOf(
                    NavAnsattRolle.kontorspesifikk(
                        Rolle.OKONOMI_BESLUTTER_ENKELTPLASS,
                        setOf(NavEnhetFixtures.Gjovik.enhetsnummer),
                    ),
                ),
            )
            MulighetsrommetTestDomain(
                ansatte = listOf(enkeltplassSaksbehandler, beslutterVedAnnetKostnadssted),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
                tilsagn = listOf(enkeltplassTilsagn),
                utbetalinger = listOf(enkeltplassUtbetaling),
                utbetalingLinjer = listOf(enkeltplassLinje),
            ) {
                setUtbetalingLinjeStatus(enkeltplassLinje, UtbetalingLinjeStatus.TIL_GODKJENNING)
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(enkeltplassUtbetaling.id, beslutterVedAnnetKostnadssted.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger.shouldBeEmpty()
        }

        test("enkeltplass-saksbehandler kan returnere, men ikke godkjenne linjen") {
            MulighetsrommetTestDomain(
                ansatte = listOf(enkeltplassSaksbehandler),
                gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
                tilsagn = listOf(enkeltplassTilsagn),
                utbetalinger = listOf(enkeltplassUtbetaling),
                utbetalingLinjer = listOf(enkeltplassLinje),
            ) {
                setUtbetalingLinjeStatus(enkeltplassLinje, UtbetalingLinjeStatus.TIL_GODKJENNING)
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(enkeltplassUtbetaling.id, enkeltplassSaksbehandler.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger shouldBe setOf(UtbetalingLinjeHandling.RETURNER)
        }

        test("attestant kan returnere, men ikke godkjenne linjen hen selv har behandlet") {
            MulighetsrommetTestDomain(
                ansatte = listOf(saksbehandler, attestant),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.TIL_GODKJENNING)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setUtbetalingLinjeStatus(
                    utbetalingLinje1,
                    UtbetalingLinjeStatus.TIL_GODKJENNING,
                    behandletAv = attestant.navIdent,
                )
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(utbetaling1.id, attestant.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger shouldBe setOf(UtbetalingLinjeHandling.RETURNER)
        }

        test("godkjent linje gir ingen handlinger") {
            MulighetsrommetTestDomain(
                ansatte = listOf(saksbehandler, attestant),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1),
                tilsagn = listOf(TilsagnFixtures.Tilsagn1),
                utbetalinger = listOf(utbetaling1.copy(status = UtbetalingStatusType.FERDIG_BEHANDLET)),
                utbetalingLinjer = listOf(utbetalingLinje1),
            ) {
                setUtbetalingLinjeStatus(utbetalingLinje1, UtbetalingLinjeStatus.GODKJENT)
            }.initialize(database.api)

            val linje = createQuery()
                .getLinjer(utbetaling1.id, attestant.navIdent, onBehalfOf)
                .shouldBeRight()
                .single()

            linje.handlinger.shouldBeEmpty()
        }
    }
})
