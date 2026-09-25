package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

/*
class TilskuddArrangorUtbetalingConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val journalforVedtaksbrev = mockk<JournalforVedtaksbrev>()
    val betalingsinformasjon = mockk<BetalingsinformasjonQuery>()

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
        ).initialize(database.api)

        coEvery { betalingsinformasjon.execute(any()) } returns Betalingsinformasjon.BBan(
            Kontonummer("12345678901"),
            null,
        )
    }

    afterEach {
        database.truncateAll()
    }

    val behandlingId = UUID.randomUUID()
    val tilskuddVedtakId = UUID.randomUUID()
    val tilskuddId = UUID.randomUUID()

    val request = TilskuddBehandlingRequest(
        id = behandlingId,
        gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
        tilskudd = listOf(
            TilskuddBehandlingRequest.TilskuddRequest(
                id = tilskuddVedtakId,
                tilskuddId = tilskuddId,
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadJournalpostId = "J-2024-001",
                soknadDato = LocalDate.of(2024, 1, 15),
                periodeStart = "2025-01-01",
                periodeSlutt = "2025-07-01",
                kostnadssted = NavEnhetNummer("0502"),
                soknadBelop = ValutaBelopRequest(belop = 100, valuta = Valuta.NOK),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = null,
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kidNummer = "116",
                belop = 100,
                kommentarIntern = null,
            ),
        ),
    )

    val godkjentHendelse = TotrinnskontrollHendelse(
        id = UUID.randomUUID(),
        entityId = behandlingId,
        type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
        status = TotrinnskontrollHendelse.Status.GODKJENT,
        behandletAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.DonaldDuck.navIdent),
        behandletTidspunkt = Instant.now(),
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent),
        besluttetTidspunkt = Instant.now(),
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
    )

    val gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))

    fun createConsumer(): TilskuddArrangorUtbetalingConsumer {
        val tilsagnService = TilsagnService(
            db = database.api,
            config = TilsagnService.Config(
                gyldigTilsagnPeriode = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to gyldigTilsagnPeriode),
            ),
            navAnsattService = mockk(relaxed = true),
        )
        val utbetalingService = UtbetalingService(
            config = UtbetalingService.Config(
                tidligstTidspunktForUtbetaling = { _, _ -> null },
            ),
            tilsagnService = tilsagnService,
            betalingsinformasjon = betalingsinformasjon,
        )
        return TilskuddArrangorUtbetalingConsumer(
            db = database.api,
            utbetalingService = utbetalingService,
            tilsagnService = tilsagnService,
        )
    }

    test("oppretter utbetaling for innvilget tilskudd til arrangør") {
        val service = TilskuddBehandlingService(
            database.api,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            gyldigJournalpostValidator(),
            mockk(relaxed = true),
        )

        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        consumer.consume(behandlingId, godkjentHendelse)

        database.run {
            val utbetaling = queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)[0]
            utbetaling.status shouldBe UtbetalingStatusType.FERDIG_BEHANDLET
            utbetaling.tilskuddstype shouldBe Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD
            val linje = queries.utbetalingLinje.getByUtbetalingId(utbetaling.id).shouldHaveSize(1)[0]
            linje.status shouldBe UtbetalingLinjeStatus.OVERFORT_TIL_UTBETALING
        }
    }

    test("behandler ikke tilskudd to ganger hvis utbetaling allerede eksisterer") {
        val service = TilskuddBehandlingService(
            database.api,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            gyldigJournalpostValidator(),
            mockk(relaxed = true),
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val consumer = createConsumer()
        consumer.consume(behandlingId, godkjentHendelse)
        consumer.consume(behandlingId, godkjentHendelse)

        database.run {
            queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)
        }
    }

    test("hopper over arrangorutbetaling når utbetaling allerede finnes") {
        val service = TilskuddBehandlingService(
            database.api,
            journalforVedtaksbrev,
            mockk(relaxed = true),
            gyldigJournalpostValidator(),
            mockk(relaxed = true),
        )
        service.upsert(request, NavAnsattFixture.DonaldDuck.navIdent).shouldBeRight()

        val existingUtbetaling = UtbetalingFixtures.utbetaling1.copy(
            gjennomforingId = request.gjennomforingId,
        )
        database.api.transaction {
            queries.utbetaling.upsert(existingUtbetaling)
            queries.tilskuddBehandling.setUtbetalingTilskuddVedtak(tilskuddVedtakId, existingUtbetaling.id)
        }

        createConsumer().consume(behandlingId, godkjentHendelse)

        database.run {
            val utbetaling = queries.utbetaling.getByTilskuddVedtak(tilskuddVedtakId).shouldNotBeNull()
            utbetaling.id shouldBe existingUtbetaling.id
            queries.utbetaling.getByGjennomforing(request.gjennomforingId).shouldHaveSize(1)
        }
    }
})


 */
