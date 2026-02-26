package no.nav.mulighetsrommet.api.tilsagn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.tilsagn.task.JournalforEnkeltplassTilsagnsbrev
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiPart
import java.time.LocalDate
import java.util.UUID

class TilsagnServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val ansatt1 = NavAnsattFixture.DonaldDuck.navIdent
    val ansatt2 = NavAnsattFixture.MikkeMus.navIdent

    val beregningFri = {
        TilsagnBeregningRequest(
            type = TilsagnBeregningType.FRI,
            valuta = Valuta.NOK,
            linjer = listOf(
                TilsagnInputLinjeRequest(
                    id = UUID.randomUUID(),
                    beskrivelse = "Beskrivelse",
                    pris = 1500.withValuta(Valuta.NOK),
                    antall = 1,
                ),
            ),
            prisbetingelser = null,
        )
    }

    val requestId = UUID.randomUUID()
    val request = TilsagnRequest(
        id = requestId,
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        type = TilsagnType.TILSAGN,
        periodeStart = "2025-01-01",
        periodeSlutt = "2025-01-31",
        kostnadssted = Gjovik.enhetsnummer,
        beregning = beregningFri(),
        kommentar = null,
    )

    beforeEach {
        MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, Gjovik, Lillehammer),
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            avtaler = listOf(AvtaleFixtures.AFT, AvtaleFixtures.ARR),
            gjennomforinger = listOf(
                GjennomforingFixtures.AFT1,
                GjennomforingFixtures.ArbeidsrettetRehabilitering,
                GjennomforingFixtures.EnkelAmo,
            ),
        ) {
            queries.ansatt.setRoller(
                ansatt1,
                setOf(NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(Gjovik.enhetsnummer))),
            )
            queries.ansatt.setRoller(
                ansatt2,
                setOf(NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(Gjovik.enhetsnummer))),
            )
        }.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    val gyldigTilsagnPeriode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))

    fun createTilsagnService(
        navAnsattService: NavAnsattService = mockk(relaxed = true),
        journalforEnkeltplassTilsagnsbrev: JournalforEnkeltplassTilsagnsbrev = mockk(relaxed = true),
    ): TilsagnService {
        return TilsagnService(
            db = database.db,
            config = TilsagnService.Config(
                bestillingTopic = "topic",
                gyldigTilsagnPeriode = mapOf(
                    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to gyldigTilsagnPeriode,
                    Tiltakskode.ARBEIDSRETTET_REHABILITERING to gyldigTilsagnPeriode,
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to gyldigTilsagnPeriode,
                ),
            ),
            navAnsattService = navAnsattService,
            journalforEnkeltplassTilsagnsbrev = journalforEnkeltplassTilsagnsbrev,
        )
    }

    context("opprett tilsagn") {
        val service = createTilsagnService()

        test("oppretter tilsagn med riktig periode og totrinnskontroll") {
            service.upsert(request, ansatt1).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))
            }

            database.run {
                queries.totrinnskontroll.getAll(requestId).shouldHaveSize(1).first().should {
                    it.behandletAv shouldBe ansatt1
                    it.type shouldBe Totrinnskontroll.Type.OPPRETT
                }
            }
        }

        test("lagrer prisbetingelser på beregnet tilsagn") {
            val nyePrisbetingelser = "Helt ferske prisbetingelser"
            val beregningInput = TilsagnBeregningRequest(
                type = TilsagnBeregningType.FRI,
                valuta = Valuta.NOK,
                linjer = listOf(
                    TilsagnInputLinjeRequest(
                        id = UUID.randomUUID(),
                        beskrivelse = "1500",
                        pris = 1500.withValuta(Valuta.NOK),
                        antall = 1,
                    ),
                ),
                prisbetingelser = nyePrisbetingelser,
            )
            val gjennomforing = GjennomforingFixtures.ArbeidsrettetRehabilitering
            service.upsert(
                request.copy(
                    gjennomforingId = gjennomforing.id,
                    periodeStart = "2025-01-01",
                    periodeSlutt = "2025-02-01",
                    beregning = beregningInput,
                ),
                ansatt1,
            ).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.beregning.input.shouldBeTypeOf<TilsagnBeregningFri.Input>().prisbetingelser shouldBe nyePrisbetingelser
            }
        }

        test("genererer løpenummer og bestillingsnummer") {
            val domain2 = MulighetsrommetTestDomain(
                gjennomforinger = listOf(
                    GjennomforingFixtures.AFT1,
                    GjennomforingFixtures.AFT1.copy(id = UUID.randomUUID()),
                ),
            ).initialize(database.db)

            val tilsagn2 = UUID.randomUUID()
            val tilsagn3 = UUID.randomUUID()

            service.upsert(
                request,
                ansatt1,
            ).shouldBeRight()
            service.upsert(
                request.copy(id = tilsagn2, beregning = beregningFri()),
                ansatt1,
            ).shouldBeRight()
            service.upsert(
                request.copy(
                    id = tilsagn3,
                    gjennomforingId = domain2.gjennomforinger[1].id,
                    beregning = beregningFri(),
                ),
                ansatt1,
            ).shouldBeRight()

            database.run {
                val aft1 = queries.gjennomforing.getGjennomforingOrError(domain2.gjennomforinger[0].id)
                queries.tilsagn.getOrError(requestId).should {
                    it.lopenummer shouldBe 1
                }

                queries.tilsagn.getOrError(tilsagn2).should {
                    it.lopenummer shouldBe 2
                    it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer.value}-2"
                }

                val aft2 = queries.gjennomforing.getGjennomforingOrError(domain2.gjennomforinger[1].id)
                queries.tilsagn.getOrError(tilsagn3).should {
                    it.lopenummer shouldBe 1
                    it.bestilling.bestillingsnummer shouldBe "A-${aft2.lopenummer.value}-1"
                }
            }
        }
    }

    context("slett tilsagn") {
        val navAnsattService = mockk<NavAnsattService>(relaxed = true)
        val service = createTilsagnService(navAnsattService)

        beforeEach {
            clearAllMocks()
        }

        test("kan ikke slette tilsagn når det er til godkjenning") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.slettTilsagn(
                requestId,
                ansatt1,
            ) shouldBeLeft listOf(FieldError.of("Kan ikke slette tilsagn som er godkjent"))
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.slettTilsagn(
                requestId,
                ansatt1,
            ) shouldBeLeft listOf(FieldError.of("Kan ikke slette tilsagn som er godkjent"))
        }

        test("kan slette tilsagn når det er returnert") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.slettTilsagn(requestId, ansatt1).shouldBeRight()

            database.run {
                queries.tilsagn.getAll() shouldHaveSize 0
            }
        }

        test("kan ikke returnere tilsagn to ganger") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ) shouldBeLeft listOf(
                FieldError.of("Tilsagnet kan ikke returneres fordi det har status Returnert"),
            )
        }

        test("skal sende notifikasjon til behandletAv når besluttetAv sletter tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.slettTilsagn(requestId, ansatt2).shouldBeRight()
            verify(exactly = 1) { navAnsattService.getNavAnsattByNavIdent(ansatt2) }

            database.run {
                queries.notifications.getAll().size shouldBe 1
            }
        }

        test("skal ikke sende notifikasjon når behandletAv sletter tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.slettTilsagn(requestId, ansatt1).shouldBeRight()

            verify(exactly = 0) { navAnsattService.getNavAnsattByNavIdent(any()) }
            database.run {
                queries.notifications.getAll().size shouldBe 0
            }
        }
    }

    context("beslutt tilsagn") {
        val service = createTilsagnService()

        test("kan ikke beslutte når ansatt mangler beslutter-rolle") {
            database.run {
                queries.ansatt.setRoller(ansatt1, setOf())
            }

            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt1,
            ) shouldBeLeft listOf(
                FieldError.of("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (Nav Gjøvik)"),
            )
        }

        test("kan ikke beslutte når ansatt bare har beslutter-rolle ved andre kostnadssteder") {
            database.run {
                queries.ansatt.setRoller(
                    ansatt1,
                    setOf(NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(Lillehammer.enhetsnummer))),
                )
            }

            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt1,
            ) shouldBeLeft listOf(
                FieldError.of("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (Nav Gjøvik)"),
            )
        }

        test("kan ikke beslutte egne opprettelser") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt1,
            ) shouldBeLeft listOf(FieldError.of("Du kan ikke beslutte et tilsagn du selv har opprettet"))
        }

        test("kan ikke beslutte to ganger") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ) shouldBeLeft listOf(FieldError.of("Tilsagnet kan ikke godkjennes fordi det har status Godkjent"))
        }

        test("godkjent tilsagn trigger melding til økonomi") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            val value = database.run { queries.kafkaProducerRecord.getRecords(50) }
                .shouldHaveSize(1)
                .first().value
            Json.decodeFromString<OkonomiBestillingMelding>(value.decodeToString())
                .shouldBeTypeOf<OkonomiBestillingMelding.Bestilling>()
                .payload.should {
                    it.behandletAv shouldBe OkonomiPart.NavAnsatt(navIdent = ansatt1)
                    it.besluttetAv shouldBe OkonomiPart.NavAnsatt(navIdent = ansatt2)
                    it.kostnadssted shouldBe request.kostnadssted
                    it.periode shouldBe Periode.fromInclusiveDates(
                        LocalDate.parse(request.periodeStart!!),
                        LocalDate.parse(request.periodeSlutt!!),
                    )
                }
        }

        xtest("godkjent enkeltplass tilsagn trigger skedulering av tilsagnsbrev") {
            val journalforEnkeltplassTilsagnsbrev = mockk<JournalforEnkeltplassTilsagnsbrev>(relaxed = true)
            val service2 = createTilsagnService(journalforEnkeltplassTilsagnsbrev = journalforEnkeltplassTilsagnsbrev)
            service2.upsert(request.copy(gjennomforingId = GjennomforingFixtures.EnkelAmo.id), ansatt1).shouldBeRight()
                .should {
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                    it.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))
                }
            service2.godkjennTilsagn(id = requestId, navIdent = ansatt2)
                .shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            verify(exactly = 1) { journalforEnkeltplassTilsagnsbrev.schedule(requestId, any()) }
        }

        test("godkjent gruppe tilsagn skal ikke trigge skedulering av tilsagnsbrev") {
            val journalforEnkeltplassTilsagnsbrev = mockk<JournalforEnkeltplassTilsagnsbrev>(relaxed = true)
            val service2 = createTilsagnService(journalforEnkeltplassTilsagnsbrev = journalforEnkeltplassTilsagnsbrev)
            service2.upsert(request.copy(gjennomforingId = GjennomforingFixtures.AFT1.id), ansatt1).shouldBeRight()
                .should {
                    it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                    it.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))
                }
            service2.godkjennTilsagn(id = requestId, navIdent = ansatt2)
                .shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            verify(exactly = 0) { journalforEnkeltplassTilsagnsbrev.schedule(requestId, any()) }
        }

        test("totrinnskontroll blir oppdatert i forbindelse med opprettelse av tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                }
            }

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe ansatt2
                    it.besluttelse shouldBe Besluttelse.AVVIST
                }
            }

            service.upsert(request, NavIdent("T888888"))
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe NavIdent("T888888")
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                }
            }

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.OPPRETT).should {
                    it.behandletAv shouldBe NavIdent("T888888")
                    it.besluttetAv shouldBe ansatt2
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                }

                queries.totrinnskontroll.getAll(requestId).shouldHaveSize(2)
            }
        }

        test("løpenummer beholdes når tilsagn blir returnert") {
            val aft1 = database.run { queries.gjennomforing.getGjennomforingOrError(GjennomforingFixtures.AFT1.id) }

            service.upsert(request, ansatt1).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.lopenummer shouldBe 1
                it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer.value}-1"
            }

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt2,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.upsert(request, NavIdent("T888888")).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.lopenummer shouldBe 1
                it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer.value}-1"
            }
        }

        test("returnere eget tilsagn") {
            val aft1 = database.run { queries.gjennomforing.getGjennomforingOrError(GjennomforingFixtures.AFT1.id) }

            service.upsert(request, ansatt1).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.lopenummer shouldBe 1
                it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer.value}-1"
            }

            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt1,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT
        }
    }

    context("annuller tilsagn") {
        val service = createTilsagnService()

        test("totrinnskontroll blir oppdatert ved annullering av tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            shouldThrow<IllegalArgumentException> {
                service.tilAnnulleringRequest(
                    id = requestId,
                    navIdent = ansatt2,
                    request = AarsakerOgForklaringRequest(
                        aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                        forklaring = "Velg et annet beløp",
                    ),
                )
            }.message shouldBe "Kan bare annullere godkjente tilsagn"

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.tilAnnulleringRequest(
                id = requestId,
                navIdent = ansatt1,
                request = AarsakerOgForklaringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ).status shouldBe TilsagnStatus.TIL_ANNULLERING

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.ANNULLER).should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                    it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP.name)
                    it.forklaring shouldBe "Velg et annet beløp"
                }
            }

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.ANNULLERT

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.ANNULLER).should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe ansatt2
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                    it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP.name)
                    it.forklaring shouldBe "Velg et annet beløp"
                }
            }
        }

        test("annullering av tilsagn trigger melding til økonomi") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            var value = database.run { queries.kafkaProducerRecord.getRecords(50) }
                .shouldHaveSize(1)
                .first().value
            val bestillingsnummer = Json.decodeFromString<OkonomiBestillingMelding>(value.decodeToString())
                .shouldBeTypeOf<OkonomiBestillingMelding.Bestilling>()
                .payload.bestillingsnummer

            service.tilAnnulleringRequest(
                id = requestId,
                navIdent = ansatt1,
                request = AarsakerOgForklaringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ).status shouldBe TilsagnStatus.TIL_ANNULLERING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.ANNULLERT

            value = database.run { queries.kafkaProducerRecord.getRecords(50) }
                .shouldHaveSize(2).elementAt(1).value
            Json.decodeFromString<OkonomiBestillingMelding>(value.decodeToString())
                .shouldBeTypeOf<OkonomiBestillingMelding.Annullering>()
                .payload.should {
                    it.bestillingsnummer shouldBe bestillingsnummer
                    it.behandletAv shouldBe OkonomiPart.NavAnsatt(navIdent = ansatt1)
                    it.besluttetAv shouldBe OkonomiPart.NavAnsatt(navIdent = ansatt2)
                }
        }

        test("kan ikke annullere eget tilsagn") {
            service.upsert(request, ansatt1).shouldBeRight()
            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight()
            service.tilAnnulleringRequest(
                id = requestId,
                navIdent = ansatt1,
                request = AarsakerOgForklaringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            )
            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt1,
            ) shouldBeLeft listOf(FieldError.of("Du kan ikke beslutte annullering du selv har opprettet"))
            database.run { queries.tilsagn.getOrError(requestId).status shouldBe TilsagnStatus.TIL_ANNULLERING }
        }
    }

    context("Gjør opp tilsagn") {
        val service = createTilsagnService()

        test("kan ikke gjøre opp egen") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING
            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.tilGjorOppRequest(
                id = requestId,
                navIdent = ansatt1,
                request = AarsakerOgForklaringRequest(aarsaker = emptyList(), forklaring = null),
            ).status shouldBe TilsagnStatus.TIL_OPPGJOR

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt1,
            ) shouldBeLeft listOf(FieldError.of("Du kan ikke beslutte oppgjør du selv har opprettet"))

            database.run {
                queries.tilsagn.getOrError(requestId).status shouldBe TilsagnStatus.TIL_OPPGJOR
            }
        }

        test("kan avvise eget oppgjør") {
            service.upsert(request, ansatt1).shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING
            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.tilGjorOppRequest(
                id = requestId,
                navIdent = ansatt1,
                request = AarsakerOgForklaringRequest(aarsaker = emptyList(), forklaring = null),
            ).status shouldBe TilsagnStatus.TIL_OPPGJOR
            service.returnerTilsagn(
                id = requestId,
                navIdent = ansatt1,
                aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                forklaring = null,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT
        }

        test("oppgjort tilsagn trigger melding til økonomi") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT
            val bestillingsnummer = database.run { queries.tilsagn.getOrError(requestId).bestilling.bestillingsnummer }

            service.tilGjorOppRequest(
                id = requestId,
                navIdent = ansatt1,
                request = AarsakerOgForklaringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).status shouldBe TilsagnStatus.TIL_OPPGJOR

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.GJOR_OPP).should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                }
            }

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.OPPGJORT

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.GJOR_OPP).should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe ansatt2
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                }
            }

            val value = database.run { queries.kafkaProducerRecord.getRecords(50) }
                .shouldHaveSize(2)
                .elementAt(1).value
            Json.decodeFromString<OkonomiBestillingMelding>(value.decodeToString())
                .shouldBeTypeOf<OkonomiBestillingMelding.GjorOppBestilling>()
                .payload.should {
                    it.bestillingsnummer shouldBe bestillingsnummer
                    it.behandletAv shouldBe OkonomiPart.NavAnsatt(navIdent = ansatt1)
                    it.besluttetAv shouldBe OkonomiPart.NavAnsatt(navIdent = ansatt2)
                }
        }

        test("systemet kan gjøre opp tilsagnet uten en ekstra part i totrinnskontroll") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.godkjennTilsagn(
                id = requestId,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            database.run {
                service.gjorOppTilsagnVedUtbetaling(id = requestId, Tiltaksadministrasjon, Tiltaksadministrasjon, this)
            }.shouldBeRight().status shouldBe TilsagnStatus.OPPGJORT

            database.run {
                queries.totrinnskontroll.getOrError(requestId, Totrinnskontroll.Type.GJOR_OPP).should {
                    it.behandletAv shouldBe Tiltaksadministrasjon
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                }

                // Verifiser at det ikke blir sendt oppgjør-melding til økonomi ved automatisk oppgjør
                queries.kafkaProducerRecord.getRecords(10).shouldHaveSize(1)
            }
        }
    }
})
