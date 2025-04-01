package no.nav.mulighetsrommet.api.tilsagn

import arrow.core.left
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFTMedSluttdato
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.VTA1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.responses.FieldError
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
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import java.time.LocalDate
import java.util.*

class TilsagnServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val minimumTilsagnPeriodeStart = LocalDate.of(2025, 1, 1)

    val ansatt1 = NavAnsattFixture.ansatt1.navIdent
    val ansatt2 = NavAnsattFixture.ansatt2.navIdent

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
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        ).initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createTilsagnService(): TilsagnService {
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
        )
    }

    context("opprett tilsagn") {
        test("tilsagn kan ikke vare over årsskiftet") {
            val service = createTilsagnService()

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
            val service = createTilsagnService()

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
            val service = createTilsagnService()

            MulighetsrommetTestDomain(
                arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(AFTMedSluttdato),
            ).initialize(database.db)

            val invalidRequest = request.copy(
                gjennomforingId = AFTMedSluttdato.id,
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
            val service = createTilsagnService()

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
            val service = createTilsagnService()

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
            val service = createTilsagnService()

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
        test("kan ikke slette tilsagn når det er til godkjenning") {
            val service = createTilsagnService()

            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.slettTilsagn(request.id) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }

        test("kan ikke slette tilsagn når det er godkjent") {
            val service = createTilsagnService()

            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt2,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT

            service.slettTilsagn(request.id) shouldBeLeft BadRequest("Kan ikke slette tilsagn som er godkjent")
        }

        test("kan slette tilsagn når det er returnert") {
            val service = createTilsagnService()

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

            service.slettTilsagn(request.id).shouldBeRight()

            service.getAll() shouldHaveSize 0
        }
    }

    context("beslutt tilsagn") {
        test("kan ikke beslutte egne opprettelser") {
            val service = createTilsagnService()

            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
                navIdent = ansatt1,
            ) shouldBe Forbidden("Kan ikke beslutte eget tilsagn").left()
        }

        test("kan ikke beslutte to ganger") {
            val service = createTilsagnService()

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
            val service = createTilsagnService()

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
                    it.kostnadssted shouldBe NavEnhetNummer(request.kostnadssted)
                    it.periode shouldBe Periode.fromInclusiveDates(request.periodeStart, request.periodeSlutt)
                }
        }

        test("totrinnskontroll blir oppdatert i forbindelse med opprettelse av tilsagn") {
            val service = createTilsagnService()

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
            val service = createTilsagnService()

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
        test("totrinnskontroll blir oppdatert ved annullering av tilsagn") {
            val service = createTilsagnService()

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
            val service = createTilsagnService()

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
    }

    context("Gjør opp tilsagn") {
        test("totrinnskontroll kreves for oppgjør av tilsagn") {
            val service = createTilsagnService()

            service.upsert(request, ansatt1)
                .shouldBeRight().status shouldBe TilsagnStatus.TIL_GODKJENNING

            service.beslutt(
                id = request.id,
                navIdent = ansatt2,
                besluttelse = BesluttTilsagnRequest.GodkjentTilsagnRequest,
            ).shouldBeRight().status shouldBe TilsagnStatus.GODKJENT
            val bestillingsnummer = database.run { queries.tilsagn.get(request.id) }!!.bestilling.bestillingsnummer

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

        test("systemet kan gjøre opp tilsagnet uten en ekstra part i totrinnskontroll") {
            val service = createTilsagnService()

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
