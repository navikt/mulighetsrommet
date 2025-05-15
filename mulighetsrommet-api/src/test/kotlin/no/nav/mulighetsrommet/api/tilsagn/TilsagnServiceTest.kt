package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Lillehammer
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navansatt.service.NavAnsattService
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.tilsagn.api.BesluttTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.api.TilAnnulleringRequest
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.OkonomiBestillingMelding
import no.nav.tiltak.okonomi.OkonomiPart
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val minimumTilsagnPeriodeStart = LocalDate.of(2025, 1, 1)

    val ansatt1 = NavAnsattFixture.DonaldDuck.navIdent
    val ansatt2 = NavAnsattFixture.MikkeMus.navIdent

    val request = TilsagnRequest(
        id = UUID.randomUUID(),
        gjennomforingId = AFT1.id,
        type = TilsagnType.TILSAGN,
        periodeStart = LocalDate.of(2025, 1, 1),
        periodeSlutt = LocalDate.of(2025, 1, 31),
        kostnadssted = Gjovik.enhetsnummer,
        beregning = TilsagnBeregningFri.Input(belop = 1),
    )

    beforeEach {
        MulighetsrommetTestDomain(
            navEnheter = listOf(NavEnhetFixtures.Innlandet, Gjovik, Lillehammer),
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
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

    fun createTilsagnService(navAnsattService: NavAnsattService = mockk(relaxed = true)): TilsagnService {
        return TilsagnService(
            db = database.db,
            config = TilsagnService.Config(
                okonomiConfig = OkonomiConfig(
                    minimumTilsagnPeriodeStart = mapOf(
                        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to minimumTilsagnPeriodeStart,
                    ),
                ),
                bestillingTopic = "topic",
            ),
            navAnsattService = navAnsattService,
        )
    }

    context("opprett tilsagn") {
        val service = createTilsagnService()

        test("tilsagn kan ikke vare over årsskiftet") {
            val invalidRequest = request.copy(
                periodeStart = LocalDate.of(2025, 1, 1),
                periodeSlutt = LocalDate.of(2026, 1, 31),
            )

            service.upsert(invalidRequest, ansatt1).shouldBeLeft() shouldBe listOf(
                FieldError(
                    pointer = "/periodeSlutt",
                    detail = "Tilsagnsperioden kan ikke vare utover årsskiftet",
                ),
            )
        }

        test("minimum dato for tilsagn må være satt for at tilsagn skal opprettes") {
            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.VTA),
                gjennomforinger = listOf(VTA1),
            ).initialize(database.db)

            val invalidRequest = request.copy(
                gjennomforingId = VTA1.id,
            )

            service.upsert(invalidRequest, ansatt1).shouldBeLeft() shouldBe listOf(
                FieldError(
                    pointer = "/periodeStart",
                    detail = "Tilsagn for tiltakstype Varig tilrettelagt arbeid i skjermet virksomhet er ikke støttet enda",
                ),
            )
        }

        test("tilsagnet kan ikke slutte etter gjennomføringen") {
            val gjennomforing = AFT1.copy(
                startDato = LocalDate.of(2025, 1, 1),
                sluttDato = LocalDate.of(2025, 2, 1),
            )

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(gjennomforing),
            ).initialize(database.db)

            val invalidRequest = request.copy(
                gjennomforingId = gjennomforing.id,
                periodeStart = LocalDate.of(2025, 1, 1),
                periodeSlutt = LocalDate.of(2025, 3, 1),
            )

            service.upsert(invalidRequest, ansatt1).shouldBeLeft() shouldBe listOf(
                FieldError(
                    pointer = "/periodeSlutt",
                    detail = "Sluttdato for tilsagnet kan ikke være etter gjennomføringsperioden",
                ),
            )
        }

        test("tilsagnet kan ikke starte før konfigurert minimum dato for tilsagn") {
            val invalidRequest = request.copy(
                periodeStart = LocalDate.of(2024, 12, 1),
                periodeSlutt = LocalDate.of(2025, 1, 31),
            )

            service.upsert(invalidRequest, ansatt1).shouldBeLeft() shouldBe listOf(
                FieldError(
                    pointer = "/periodeStart",
                    detail = "Minimum startdato for tilsagn til Arbeidsforberedende trening (AFT) er 01.01.2025",
                ),
            )
        }

        test("oppretter tilsagn med riktig periode og totrinnskontroll") {
            service.upsert(request, ansatt1).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.periode shouldBe Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1))
            }

            database.run {
                queries.totrinnskontroll.getAll(request.id).shouldHaveSize(1).first().should {
                    it.behandletAv shouldBe ansatt1
                    it.type shouldBe Totrinnskontroll.Type.OPPRETT
                }
            }
        }

        test("genererer løpenummer og bestillingsnummer") {
            val domain2 = MulighetsrommetTestDomain(
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFT1, AFT1.copy(id = UUID.randomUUID())),
            ).initialize(database.db)

            val tilsagn2 = UUID.randomUUID()
            val tilsagn3 = UUID.randomUUID()

            service.upsert(
                request,
                ansatt1,
            ).shouldBeRight()
            service.upsert(
                request.copy(id = tilsagn2),
                ansatt1,
            ).shouldBeRight()
            service.upsert(
                request.copy(id = tilsagn3, gjennomforingId = domain2.gjennomforinger[1].id),
                ansatt1,
            ).shouldBeRight()

            database.run {
                val aft1 = queries.gjennomforing.get(domain2.gjennomforinger[0].id).shouldNotBeNull()
                queries.tilsagn.get(request.id).shouldNotBeNull().should {
                    it.lopenummer shouldBe 1
                }

                queries.tilsagn.get(tilsagn2).shouldNotBeNull().should {
                    it.lopenummer shouldBe 2
                    it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer}-2"
                }

                val aft2 = queries.gjennomforing.get(domain2.gjennomforinger[1].id).shouldNotBeNull()
                queries.tilsagn.get(tilsagn3).shouldNotBeNull().should {
                    it.lopenummer shouldBe 1
                    it.bestilling.bestillingsnummer shouldBe "A-${aft2.lopenummer}-1"
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

            service.slettTilsagn(request.id, ansatt1) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.slettTilsagn(request.id, ansatt1) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }

        test("kan slette tilsagn når det er returnert") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                request.id,
                BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
                ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.slettTilsagn(request.id, ansatt1).shouldBeRight()

            service.getAll() shouldHaveSize 0
        }

        test("skal sende notifikasjon til behandletAv når besluttetAv sletter tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                request.id,
                BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
                ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.slettTilsagn(request.id, ansatt2).shouldBeRight()
            verify(exactly = 1) { navAnsattService.getNavAnsattByNavIdent(ansatt2) }

            database.run {
                queries.notifications.getAll().size shouldBe 1
            }
        }

        test("skal ikke sende notifikasjon når behandletAv sletter tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                request.id,
                BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
                ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.slettTilsagn(request.id, ansatt1).shouldBeRight()
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

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt1,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>().should {
                it.errors shouldContain FieldError.root("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (Nav Gjøvik)")
            }
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

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt1,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>().should {
                it.errors shouldContain FieldError.root("Du kan ikke beslutte tilsagnet fordi du mangler budsjettmyndighet ved tilsagnets kostnadssted (Nav Gjøvik)")
            }
        }

        test("kan ikke beslutte egne opprettelser") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt1,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>().should {
                it.errors shouldContain FieldError.root("Du kan ikke beslutte et tilsagn du selv har opprettet")
            }
        }

        test("kan ikke beslutte to ganger") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ) shouldBe BadRequest("Tilsagnet kan ikke besluttes fordi det har status GODKJENT").left()
        }

        test("godkjent tilsagn trigger melding til økonomi") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
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
                    it.periode shouldBe Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)
                }
        }

        test("totrinnskontroll blir oppdatert i forbindelse med opprettelse av tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.OPPRETT).shouldNotBeNull().should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                }
            }

            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.OPPRETT).shouldNotBeNull().should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe ansatt2
                    it.besluttelse shouldBe Besluttelse.AVVIST
                }
            }

            service.upsert(request, NavIdent("T888888"))
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.OPPRETT).shouldNotBeNull().should {
                    it.behandletAv shouldBe NavIdent("T888888")
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                }
            }

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.OPPRETT).shouldNotBeNull().should {
                    it.behandletAv shouldBe NavIdent("T888888")
                    it.besluttetAv shouldBe ansatt2
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                }

                queries.totrinnskontroll.getAll(request.id).shouldHaveSize(2)
            }
        }

        test("løpenummer beholdes når tilsagn blir returnert") {
            val aft1 = database.run { queries.gjennomforing.get(AFT1.id).shouldNotBeNull() }

            service.upsert(request, ansatt1).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.lopenummer shouldBe 1
                it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer}-1"
            }

            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.AvvistTilsagnRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).shouldBeRight().status shouldBe TilsagnStatus.RETURNERT

            service.upsert(request, NavIdent("T888888")).shouldBeRight().should {
                it.status shouldBe TilsagnStatus.TIL_GODKJENNING
                it.lopenummer shouldBe 1
                it.bestilling.bestillingsnummer shouldBe "A-${aft1.lopenummer}-1"
            }
        }
    }

    context("annuller tilsagn") {
        val service = createTilsagnService()

        test("totrinnskontroll blir oppdatert ved annullering av tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            shouldThrow<IllegalArgumentException> {
                service.tilAnnulleringRequest(
                    id = request.id,
                    navIdent = ansatt2,
                    request = TilAnnulleringRequest(
                        aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                        forklaring = "Velg et annet beløp",
                    ),
                )
            }.message shouldBe "Kan bare annullere godkjente tilsagn"

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.tilAnnulleringRequest(
                id = request.id,
                navIdent = ansatt1,
                request = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ).status shouldBe TilsagnStatus.TIL_ANNULLERING

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.ANNULLER).shouldNotBeNull().should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                    it.aarsaker shouldBe listOf(TilsagnStatusAarsak.FEIL_BELOP.name)
                    it.forklaring shouldBe "Velg et annet beløp"
                }
            }

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.ANNULLERT

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.ANNULLER).shouldNotBeNull().should {
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

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            var value = database.run { queries.kafkaProducerRecord.getRecords(50) }
                .shouldHaveSize(1)
                .first().value
            val bestillingsnummer = Json.decodeFromString<OkonomiBestillingMelding>(value.decodeToString())
                .shouldBeTypeOf<OkonomiBestillingMelding.Bestilling>()
                .payload.bestillingsnummer

            service.tilAnnulleringRequest(
                id = request.id,
                navIdent = ansatt1,
                request = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            ).status shouldBe TilsagnStatus.TIL_ANNULLERING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
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
            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight()
            service.tilAnnulleringRequest(
                id = request.id,
                navIdent = ansatt1,
                request = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = "Velg et annet beløp",
                ),
            )
            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt1,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>() should {
                it.errors shouldBe listOf(FieldError.root("Du kan ikke beslutte annullering du selv har opprettet"))
            }
            database.run { queries.tilsagn.getOrError(request.id).status shouldBe TilsagnStatus.TIL_ANNULLERING }
        }
    }

    context("Gjør opp tilsagn") {
        val service = createTilsagnService()

        test("totrinnskontroll kreves for oppgjør av tilsagn") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT
            val bestillingsnummer = database.run { queries.tilsagn.getOrError(request.id).bestilling.bestillingsnummer }

            service.tilGjorOppRequest(
                id = request.id,
                navIdent = ansatt1,
                request = TilAnnulleringRequest(
                    aarsaker = listOf(TilsagnStatusAarsak.FEIL_BELOP),
                    forklaring = null,
                ),
            ).status shouldBe TilsagnStatus.TIL_OPPGJOR

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.GJOR_OPP).shouldNotBeNull().should {
                    it.behandletAv shouldBe ansatt1
                    it.besluttetAv shouldBe null
                    it.besluttelse shouldBe null
                }
            }

            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
            ).shouldBeRight().status shouldBe TilsagnStatus.OPPGJORT

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.GJOR_OPP).shouldNotBeNull().should {
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

        test("kan ikke gjøre opp egen") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING
            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.tilGjorOppRequest(
                id = request.id,
                navIdent = ansatt1,
                request = TilAnnulleringRequest(aarsaker = emptyList(), forklaring = null),
            ).status shouldBe TilsagnStatus.TIL_OPPGJOR
            service.beslutt(
                id = request.id,
                navIdent = ansatt1,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
            ).shouldBeLeft().shouldBeTypeOf<ValidationError>() should {
                it.errors shouldBe listOf(FieldError.root("Du kan ikke beslutte oppgjør du selv har opprettet"))
            }
            database.run { queries.tilsagn.getOrError(request.id).status shouldBe TilsagnStatus.TIL_OPPGJOR }
        }

        test("systemet kan gjøre opp tilsagnet uten en ekstra part i totrinnskontroll") {
            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            database.db.session {
                service.gjorOppAutomatisk(id = request.id, this)
            }.status shouldBe TilsagnStatus.OPPGJORT

            database.run {
                queries.totrinnskontroll.get(request.id, Totrinnskontroll.Type.GJOR_OPP).shouldNotBeNull().should {
                    it.behandletAv shouldBe Tiltaksadministrasjon
                    it.besluttetAv shouldBe Tiltaksadministrasjon
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                }
            }
        }
    }
})
