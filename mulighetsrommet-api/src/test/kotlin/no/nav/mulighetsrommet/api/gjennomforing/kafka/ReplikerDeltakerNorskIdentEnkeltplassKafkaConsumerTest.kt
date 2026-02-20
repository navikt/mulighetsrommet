package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures.createAmtDeltakerDto
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import org.junit.jupiter.api.assertThrows

class ReplikerDeltakerNorskIdentEnkeltplassKafkaConsumerTest : FunSpec({
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

    fun createConsumer(): ReplikerDeltakerNorskIdentEnkeltplassKafkaConsumer {
        return ReplikerDeltakerNorskIdentEnkeltplassKafkaConsumer(db = database.db)
    }

    test("noop når gjennomføring er av typen AVTALE") {
        val deltakerConsumer = createConsumer()

        val deltaker = createAmtDeltakerDto(
            gjennomforingId = Oppfolging1.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getAll(search = "12345678910").items.shouldBeEmpty()
        }
    }

    test("feiler hvis deltaker ikke allerede er lagret i db") {
        val deltakerConsumer = createConsumer()

        val deltaker = createAmtDeltakerDto(
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.VENTER_PA_OPPSTART,
            personIdent = "12345678910",
        )

        val exception = assertThrows<IllegalStateException> {
            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))
        }

        exception.message shouldBe "Deltaker med id=${deltaker.id} har ikke blitt replikert ennå"
    }

    test("lagrer norsk ident for fritekstsøk på gjennomføring") {
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
            queries.gjennomforing.getAll(search = "12345678910").items.shouldHaveSize(1).should { (first) ->
                first.id shouldBe EnkelAmo.id
            }
        }
    }
})
