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
import no.nav.mulighetsrommet.api.avtale.mapper.prisbetingelser
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.brreg.BrregError
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class GjennomforingRequestKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        tiltakstyper = listOf(TiltakstypeFixtures.Amo),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
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
            tiltakstyper = TiltakstypeService(config = tiltakstypeConfig, db = database.db),
            enkeltplasser = enkeltplasser,
        )
    }

    context("OpprettGjennomforing") {
        val service = GjennomforingEnkeltplassService(
            GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            database.db,
            mockk(),
            TiltakstypeService(TiltakstypeService.Config(), database.db),
        )

        val gjennomforingId = UUID.randomUUID()

        val request = GjennomforingRequestPayload.OpprettEnkeltplass(
            gjennomforingId = gjennomforingId,
            tiltakskode = Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            prisinformasjon = "prisinformasjon",
            organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
            kostnadssted = NavEnhetNummer("0400"),
        )

        test("oppretter enkeltplass-gjennomføring når arrangør finnes i databasen") {
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorFixtures.underenhet1.right()

            val consumer = createConsumer(service, arrangorer)
            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequestPayload>(request))

            service.get(gjennomforingId).shouldNotBeNull().should {
                it.id shouldBe gjennomforingId
                it.status shouldBe GjennomforingStatusType.GJENNOMFORES
                it.arrangor.id shouldBe ArrangorFixtures.underenhet1.id
                it.kostnadssted.enhetsnummer shouldBe NavEnhetNummer("0400")
            }
        }

        test("kaster feil dersom arrangør ikke kan hentes fra brreg") {
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorError.BrregError(BrregError.NotFound).left()

            val consumer = createConsumer(service, arrangorer)

            shouldThrowExactly<IllegalStateException> {
                consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequestPayload>(request))
            }

            service.get(gjennomforingId).shouldBeNull()
        }

        test("oppretter ikke duplikat dersom gjennomføringen allerede eksisterer") {
            val arrangorer = mockk<ArrangorService>()
            coEvery {
                arrangorer.getArrangorOrSyncFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer)
            } returns ArrangorFixtures.underenhet1.right()

            val consumer = createConsumer(service, arrangorer)

            consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequestPayload>(request))

            val requestMedAndrePrisbetingelser = GjennomforingRequestPayload.OpprettEnkeltplass(
                gjennomforingId = gjennomforingId,
                tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                prisinformasjon = "andre prisbetingelser",
                organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                kostnadssted = NavEnhetNummer("0400"),
            )
            consumer.consume(
                gjennomforingId,
                Json.encodeToJsonElement<GjennomforingRequestPayload>(requestMedAndrePrisbetingelser),
            )

            service.get(gjennomforingId).shouldNotBeNull().should {
                it.prismodell.prisbetingelser() shouldBe request.prisinformasjon
            }
        }

        test("kaster feil dersom tiltakskoden ikke er migrert") {
            val ikkeMigrertConfig = TiltakstypeService.Config(features = emptyMap())
            val consumer = createConsumer(service, tiltakstypeConfig = ikkeMigrertConfig)

            shouldThrowExactly<IllegalArgumentException> {
                consumer.consume(gjennomforingId, Json.encodeToJsonElement<GjennomforingRequestPayload>(request))
            }

            service.get(gjennomforingId).shouldBeNull()
        }
    }
})
