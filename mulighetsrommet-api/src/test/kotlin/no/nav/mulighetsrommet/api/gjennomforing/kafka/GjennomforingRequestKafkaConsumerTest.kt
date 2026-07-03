package no.nav.mulighetsrommet.api.gjennomforing.kafka

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.arrangor.ArrangorError
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.brreg.BrregError
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
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createConsumer(
        enkeltplasser: GjennomforingEnkeltplassService,
        arrangorer: ArrangorService = mockk(),
        tiltakstypeConfig: TiltakstypeService.Config = TiltakstypeService.Config(
            features = mapOf(Tiltakskode.ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT)),
        ),
    ): GjennomforingRequestKafkaConsumer {
        return GjennomforingRequestKafkaConsumer(
            arrangorer = arrangorer,
            tiltakstyper = TiltakstypeService(tiltakstypeConfig, database.db),
            enkeltplasser = enkeltplasser,
        )
    }

    context("EnkeltplassUtkast") {
        val service = GjennomforingEnkeltplassService(
            database.db,
            mockk(),
            TiltakstypeService(TiltakstypeService.Config(), database.db),
        )

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
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorFixtures.underenhet1.right()

            val consumer = createConsumer(service, arrangorer)
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
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorError.BrregError(BrregError.NotFound).left()

            val consumer = createConsumer(service, arrangorer)

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
        val service = GjennomforingEnkeltplassService(
            database.db,
            mockk(),
            TiltakstypeService(TiltakstypeService.Config(), database.db),
        )

        val gjennomforingId = UUID.randomUUID()
        val payload = UpsertEnkeltplass(
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            ansvarligEnhet = NavEnhetNummer("0400"),
            opprettetAv = NavIdent("B123456"),
            prisinformasjon = EnkeltplassPrisinformasjon.Anskaffelse(pris = 10000),
            kategorisering = null,
        )
        val request = GjennomforingRequest.EnkeltplassSoktInn(gjennomforingId, null, payload)

        test("oppretter gjennomforing og sender økonomi til godkjenning") {
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorFixtures.underenhet1.right()

            val consumer = createConsumer(service, arrangorer)
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
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorError.BrregError(BrregError.NotFound).left()

            val consumer = createConsumer(service, arrangorer)

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
})
