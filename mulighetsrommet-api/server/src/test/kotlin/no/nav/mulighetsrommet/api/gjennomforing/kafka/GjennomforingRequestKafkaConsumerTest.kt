package no.nav.mulighetsrommet.api.gjennomforing.kafka

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorError
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorIfMissing
import no.nav.mulighetsrommet.admin.arrangor.SyncArrangorUseCase
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class GjennomforingRequestKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        tiltakstyper = listOf(TiltakstypeFixtures.Amo),
    )

    beforeEach {
        domain.initialize(database.api)
    }

    afterEach {
        database.truncateAll()
    }

    val mockSyncArrangor = mockk<SyncArrangorUseCase> {
        coEvery {
            execute(SyncArrangorIfMissing(ArrangorFixtures.underenhet1.organisasjonsnummer))
        } returns ArrangorFixtures.underenhet1.right()
    }

    fun createConsumer(
        enkeltplasser: GjennomforingEnkeltplassService,
        arrangorer: SyncArrangorUseCase = mockSyncArrangor,
        tiltakstypeConfig: TiltakstypeService.Config = TiltakstypeService.Config(
            features = mapOf(Tiltakskode.ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT)),
        ),
    ): GjennomforingRequestKafkaConsumer {
        return GjennomforingRequestKafkaConsumer(
            arrangorer = arrangorer,
            tiltakstyper = TiltakstypeService(tiltakstypeConfig, database.admin),
            enkeltplasser = enkeltplasser,
        )
    }

    fun createService(): GjennomforingEnkeltplassService {
        return GjennomforingEnkeltplassService(
            database.api,
            mockk(),
            TiltakstypeService(TiltakstypeService.Config(), database.admin),
        )
    }

    context("EnkeltplassUtkast") {
        val service = createService()

        val gjennomforingId = UUID.randomUUID()
        val payload = UpsertEnkeltplass(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            ansvarligEnhet = NavEnhetNummer("0400"),
            opprettetAv = NavIdent("B123456"),
            prisinformasjon = EnkeltplassPrisinformasjon.Anskaffelse(pris = 10000),
            kategorisering = null,
        )
        val request = GjennomforingRequest.EnkeltplassUtkast(gjennomforingId, payload)

        test("oppretter gjennomforing uten å sende økonomi til godkjenning") {

            val consumer = createConsumer(service)
            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))

            service.get(gjennomforingId).shouldNotBeNull().should { (gjennomforing, okonomi) ->
                gjennomforing.id shouldBe gjennomforingId
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
                gjennomforing.arrangor.id shouldBe ArrangorFixtures.underenhet1.id
                gjennomforing.ansvarligEnhet.enhetsnummer shouldBe NavEnhetNummer("0400")
                okonomi.shouldBeNull()
            }
        }

        test("kaster feil dersom arrangør ikke kan hentes fra brreg") {
            val mockArrangorIkkeFunnet = mockk<SyncArrangorUseCase> {
                coEvery {
                    execute(SyncArrangorIfMissing(ArrangorFixtures.underenhet1.organisasjonsnummer))
                } returns SyncArrangorError.Enhetsregister(EnhetsregisterError.IkkeFunnet).left()
            }

            val consumer = createConsumer(service, mockArrangorIkkeFunnet)

            shouldThrowExactly<IllegalStateException> {
                consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))
            }

            service.get(gjennomforingId).shouldBeNull()
        }

        test("kaster feil dersom tiltakskoden ikke er migrert") {
            val ikkeMigrertConfig = TiltakstypeService.Config(features = emptyMap())
            val consumer = createConsumer(service, tiltakstypeConfig = ikkeMigrertConfig)

            shouldThrowExactly<IllegalArgumentException> {
                consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))
            }

            service.get(gjennomforingId).shouldBeNull()
        }
    }

    context("EnkeltplassSoktInn") {
        val service = createService()

        val gjennomforingId = UUID.randomUUID()
        val payload = UpsertEnkeltplass(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            ansvarligEnhet = NavEnhetNummer("0400"),
            opprettetAv = NavIdent("B123456"),
            prisinformasjon = EnkeltplassPrisinformasjon.Anskaffelse(pris = 10000),
            kategorisering = null,
        )
        val totrinnskontroll = GjennomforingRequest.Totrinnskontroll(UUID.randomUUID(), NavIdent("B123456"))
        val request = GjennomforingRequest.EnkeltplassSoktInn(gjennomforingId, totrinnskontroll, payload)

        test("oppretter gjennomforing og sender økonomi til godkjenning") {
            val consumer = createConsumer(service)
            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))

            service.get(gjennomforingId).shouldNotBeNull().should { (gjennomforing, okonomi) ->
                gjennomforing.id shouldBe gjennomforingId
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
                gjennomforing.arrangor.id shouldBe ArrangorFixtures.underenhet1.id
                gjennomforing.ansvarligEnhet.enhetsnummer shouldBe NavEnhetNummer("0400")
                okonomi.shouldNotBeNull().behandletAv shouldBe NavIdent("B123456")
            }
        }

        test("kaster feil dersom arrangør ikke kan hentes fra brreg") {
            val mockArrangorIkkeFunnet = mockk<SyncArrangorUseCase> {
                coEvery {
                    execute(SyncArrangorIfMissing(ArrangorFixtures.underenhet1.organisasjonsnummer))
                } returns SyncArrangorError.Enhetsregister(EnhetsregisterError.IkkeFunnet).left()
            }

            val consumer = createConsumer(service, mockArrangorIkkeFunnet)

            shouldThrowExactly<IllegalStateException> {
                consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))
            }

            service.get(gjennomforingId).shouldBeNull()
        }

        test("kaster feil dersom tiltakskoden ikke er migrert") {
            val ikkeMigrertConfig = TiltakstypeService.Config(features = emptyMap())
            val consumer = createConsumer(service, tiltakstypeConfig = ikkeMigrertConfig)

            shouldThrowExactly<IllegalArgumentException> {
                consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))
            }

            service.get(gjennomforingId).shouldBeNull()
        }
    }

    context("EnkeltplassEndreInnhold") {
        val service = createService()

        val consumer = createConsumer(service)

        val gjennomforingId = UUID.randomUUID()
        val utkastPayload = UpsertEnkeltplass(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            ansvarligEnhet = NavEnhetNummer("0400"),
            opprettetAv = NavIdent("B123456"),
            prisinformasjon = EnkeltplassPrisinformasjon.Anskaffelse(pris = 10000),
            kategorisering = null,
        )

        test("oppdaterer kategorisering på eksisterende enkeltplass") {
            val utkast = GjennomforingRequest.EnkeltplassUtkast(gjennomforingId, utkastPayload)
            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(utkast))
            service.get(gjennomforingId).shouldNotBeNull()

            val endreInnholdRequest = GjennomforingRequest.EnkeltplassEndreInnhold(gjennomforingId, payload = null)
            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(endreInnholdRequest))

            service.get(gjennomforingId).shouldNotBeNull().should { (gjennomforing, _) ->
                gjennomforing.id shouldBe gjennomforingId
            }
        }

        test("kaster feil dersom gjennomforing ikke eksisterer") {
            val ikkeEksisterendeId = UUID.randomUUID()
            val request = GjennomforingRequest.EnkeltplassEndreInnhold(ikkeEksisterendeId, payload = null)

            shouldThrowExactly<IllegalStateException> {
                consumer.consume(ikkeEksisterendeId, Json.encodeToJsonElement<GjennomforingRequest>(request))
            }
        }
    }

    context("EnkeltplassEndrePrisinformasjon") {
        val service = createService()

        val consumer = createConsumer(service)

        val gjennomforingId = UUID.randomUUID()
        val utkastPayload = UpsertEnkeltplass(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            ansvarligEnhet = NavEnhetNummer("0400"),
            opprettetAv = NavIdent("B123456"),
            prisinformasjon = EnkeltplassPrisinformasjon.Anskaffelse(pris = 10000),
            kategorisering = null,
        )
        val totrinnskontroll = GjennomforingRequest.Totrinnskontroll(UUID.randomUUID(), NavIdent("B123456"))

        beforeEach {
            val soktInn = GjennomforingRequest.EnkeltplassSoktInn(gjennomforingId, totrinnskontroll, utkastPayload)
            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(soktInn))
        }

        test("oppdaterer prismodell automatisk når økonomi ikke er godkjent") {
            val request = GjennomforingRequest.EnkeltplassEndrePrisinformasjon(
                gjennomforingId = gjennomforingId,
                totrinnskontroll = GjennomforingRequest.Totrinnskontroll(UUID.randomUUID(), NavIdent("B123456")),
                payload = EnkeltplassPrisinformasjon.Anskaffelse(pris = 20000),
            )

            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequest>(request))

            service.get(gjennomforingId).shouldNotBeNull().should { (gjennomforing, _) ->
                gjennomforing.prismodell.shouldBeTypeOf<Prismodell.AnnenAvtaltPris>().totalbelop shouldBe 20000
            }
        }

        test("kaster feil dersom gjennomforing ikke eksisterer") {
            val ikkeEksisterendeId = UUID.randomUUID()
            val request = GjennomforingRequest.EnkeltplassEndrePrisinformasjon(
                gjennomforingId = ikkeEksisterendeId,
                totrinnskontroll = GjennomforingRequest.Totrinnskontroll(UUID.randomUUID(), NavIdent("B123456")),
                payload = EnkeltplassPrisinformasjon.Anskaffelse(pris = 5000),
            )

            shouldThrowExactly<IllegalStateException> {
                consumer.consume(ikkeEksisterendeId, Json.encodeToJsonElement<GjennomforingRequest>(request))
            }
        }
    }
})
