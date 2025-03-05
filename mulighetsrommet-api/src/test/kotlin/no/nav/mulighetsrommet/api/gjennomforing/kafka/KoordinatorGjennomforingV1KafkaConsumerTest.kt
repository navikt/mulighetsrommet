package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.NavIdent
import org.intellij.lang.annotations.Language
import java.util.*

class KoordinatorGjennomforingV1KafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
    )

    beforeTest {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createConsumer(): AmtKoordinatorGjennomforingV1KafkaConsumer {
        return AmtKoordinatorGjennomforingV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(
                id = "amt-koordinator-deltakerliste-tilgang",
                topic = "amt.tiltakskoordinator-deltakerliste-tilgang-v1",
            ),
            db = database.db,
        )
    }

    fun createMelding(
        id: UUID,
        ident: NavIdent,
        gjennomforingId: UUID,
    ): AmtKoordinatorGjennomforingV1KafkaConsumer.Melding {
        return AmtKoordinatorGjennomforingV1KafkaConsumer.Melding(
            id = id,
            navIdent = ident,
            gjennomforingId = gjennomforingId,
        )
    }

    fun getNavIdentFromRow(id: UUID): String? {
        @Language("PostgreSQL")
        val query = """
                select nav_ident
                from gjennomforing_koordinator
                where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id,
        )

        return database.run {
            session.single(queryOf(query, params)) {
                it.string("nav_ident")
            }
        }
    }

    context("Konsumering av Koordinator-kobling fra Komet") {
        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
        ).initialize(database.db)

        val consumer = createConsumer()

        test("Should process valid KoordinatorTiltaksgjennomforingV1 message successfully") {
            val message =
                createMelding(
                    id = UUID.randomUUID(),
                    ident = NavIdent("Z123456"),
                    gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                )

            consumer.consume(message.gjennomforingId.toString(), Json.encodeToJsonElement(message))

            getNavIdentFromRow(message.id) shouldBe message.navIdent.value
        }

        test("Should process tombstone message successfully") {
            val key = UUID.fromString("5e5de632-c696-4584-a18f-eb9c1a6362ca")
            val message =
                createMelding(
                    id = key,
                    ident = NavIdent("Z123456"),
                    gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                )

            consumer.consume(key.toString(), Json.encodeToJsonElement(message))

            getNavIdentFromRow(key) shouldBe message.navIdent.value

            val tombstone = null
            consumer.consume(key.toString(), Json.encodeToJsonElement(tombstone))

            getNavIdentFromRow(key) shouldBe null
        }
    }
})
