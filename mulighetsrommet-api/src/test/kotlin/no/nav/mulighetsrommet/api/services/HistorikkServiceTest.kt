package no.nav.mulighetsrommet.api.services

/*
class HistorikkServiceTest : FunSpec({
    testOrder = TestCaseOrder.Sequential

    val arrangorService: ArrangorService = mockk()
    val veilarbarenaClient: VeilarbarenaClient = mockk()

    val listener = FlywayDatabaseListener(createApiDatabaseTestSchema())

    register(listener)

    beforeSpec {
        val arenaService = ArenaService(listener.db)

        val tiltakstype = AdapterTiltak(
            navn = "Arbeidstrening",
            innsatsgruppe = 1,
            tiltakskode = "ARBTREN",
            fraDato = LocalDateTime.now(),
            tilDato = LocalDateTime.now().plusYears(1)
        )

        val tiltaksgjennomforing = AdapterTiltaksgjennomforing(
            navn = "Arbeidstrening",
            arrangorId = 1,
            tiltakskode = "ARBTREN",
            id = 123,
            sakId = 123
        )

        val deltaker = AdapterTiltakdeltaker(
            id = 123,
            tiltaksgjennomforingId = 123,
            personId = 111,
            status = Deltakerstatus.VENTER
        )

        val sak = AdapterSak(
            id = 123,
            lopenummer = 3,
            aar = 2022
        )

        arenaService.upsertTiltakstype(tiltakstype)
        arenaService.upsertTiltaksgjennomforing(tiltaksgjennomforing)
        arenaService.upsertDeltaker(deltaker)
        arenaService.updateTiltaksgjennomforingWithSak(sak)
    }

    test("hei") {
        val bedriftsnavn = "Bedriftsnavn"
        every { runBlocking { arrangorService.hentArrangorNavn(1) } } returns bedriftsnavn
        every {
            runBlocking { veilarbarenaClient.hentPersonIdForFnr(any(), any()) }
        } returns "111"

        val historikkService =
            HistorikkService(listener.db, veilarbarenaClient, arrangorService)

        val forventetHistorikk = listOf(
            HistorikkForDeltakerDTO(
                id = "1",
                fraDato = null,
                tilDato = null,
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Arbeidstrening",
                tiltaksnummer = "3",
                tiltakstype = "Arbeidstrening",
                arrangor = bedriftsnavn
            )
        )

        historikkService.hentHistorikkForBruker(
            "fnr",
            null
        ) shouldBe forventetHistorikk
    }
})
*/
