package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures.createAmtDeltakerDto
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdentHasher

class ReplikerDeltakerEnkeltplassKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(Oppfolging1, EnkelAmo),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createConsumer(
        deltakerClient: AmtDeltakerClient = mockk(),
    ): ReplikerDeltakerEnkeltplassKafkaConsumer {
        return ReplikerDeltakerEnkeltplassKafkaConsumer(
            db = database.db,
            service = GjennomforingEnkeltplassService(
                GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
                database.db,
                deltakerClient,
            ),
        )
    }

    test("lagrer hash av norsk ident for fritekstsøk på gjennomføring av typen ENKELTPLASS") {
        val domain = MulighetsrommetTestDomain(
            deltakere = listOf(
                DeltakerFixtures.createDeltakerDbo(gjennomforingId = EnkelAmo.id),
            ),
        ).initialize(database.db)

        val deltakerConsumer = createConsumer()

        val deltaker = createAmtDeltakerDto(
            id = domain.deltakere[0].id,
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getAll(search = "12345678910").items.shouldBeEmpty()

            queries.gjennomforing.getAll(
                search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
            ).items.shouldHaveSize(1).first().id shouldBe EnkelAmo.id
        }
    }

    test("fjerner norsk ident fra fritekstsøk når deltaker er FEILREGISTRERT") {
        val domain = MulighetsrommetTestDomain(
            deltakere = listOf(
                DeltakerFixtures.createDeltakerDbo(gjennomforingId = EnkelAmo.id),
            ),
        ).initialize(database.db)

        val deltakerConsumer = createConsumer()

        val feilregistrertDeltaker = createAmtDeltakerDto(
            id = domain.deltakere[0].id,
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FEILREGISTRERT,
            personIdent = "12345678910",
        )
        deltakerConsumer.consume(feilregistrertDeltaker.id, Json.encodeToJsonElement(feilregistrertDeltaker))

        database.run {
            queries.gjennomforing.getAll(
                search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
            ).items.shouldBeEmpty()
        }
    }

    test("lagrer ikke norsk ident for fritekstsøk når gjennomføring er av typen AVTALE") {
        val deltakerConsumer = createConsumer()

        val deltaker = createAmtDeltakerDto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getAll(
                search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
            ).items.shouldBeEmpty()
        }
    }
})
