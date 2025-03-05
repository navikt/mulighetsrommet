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

class KoordinatorTiltaksgjennomforingV1KafkaConsumerTest : FunSpec({
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

    fun createConsumer(): AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer {
        return AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer(
            config = KafkaTopicConsumer.Config(
                id = "amt-koordinator-deltakerliste-tilgang",
                topic = "amt.tiltakskoordinator-deltakerliste-tilgang-v1"
            ),
            db = database.db,
        )
    }

    fun createMelding(
        id: UUID,
        ident: NavIdent,
        gjennomforingId: UUID,
    ): AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer.Melding {
        return AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer.Melding(
            id = id,
            navIdent = ident,
            gjennomforingId = gjennomforingId,
        )
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
                    gjennomforingId = GjennomforingFixtures.Oppfolging1.id
                )

            consumer.consume(message.gjennomforingId.toString(), Json.encodeToJsonElement(message))

            @Language("PostgreSQL")
            val query = """
                select nav_ident
                from gjennomforing_koordinator
                where gjennomforing_id = :gjennomforing_id::uuid
            """.trimIndent()

            val params = mapOf(
                "gjennomforing_id" to message.gjennomforingId,
            )

            database.run {
                session.single(queryOf(query, params)) {
                    it.string("nav_ident")
                } shouldBe message.navIdent.value
            }
        }

        test("Should process tombstone message successfully") {
            val key = UUID.fromString("5e5de632-c696-4584-a18f-eb9c1a6362ca")
            val message =
                createMelding(
                    id = key,
                    ident = NavIdent("Z123456"),
                    gjennomforingId = GjennomforingFixtures.Oppfolging1.id
                )

            consumer.consume(key.toString(), Json.encodeToJsonElement(message))

            @Language("PostgreSQL")
            val query = """
                select nav_ident
                from gjennomforing_koordinator
                where id = :id::uuid
            """.trimIndent()

            val params = mapOf(
                "id" to key
            )

            database.run {
                session.single(queryOf(query, params)) {
                    it.string("nav_ident")
                } shouldBe message.navIdent.value
            }

            val tombstone = null
            consumer.consume(key.toString(), Json.encodeToJsonElement(tombstone))


            @Language("PostgreSQL")
            val getQuery = """
                select nav_ident
                from gjennomforing_koordinator
                where id = :id::uuid
            """.trimIndent()

            val getParams = mapOf(
                "id" to key,
            )

            database.run {
                session.single(queryOf(getQuery, getParams)) {
                    it.string("nav_ident")
                } shouldBe null
            }
        }
    }
})
