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
                id = "amt-koordinator-gjennomforing",
                topic = "amt-koordinator-gjennomforing",
            ),
            db = database.db,
        )
    }

    fun createMelding(
        ident: NavIdent,
        gjennomforingId: UUID,
    ): AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer.Melding {
        return AmtKoordinatorTiltaksgjennomforingV1KafkaConsumer.Melding(
            navIdent = ident,
            tiltaksgjennomforingId = gjennomforingId,
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
                createMelding(ident = NavIdent("Z123456"), gjennomforingId = GjennomforingFixtures.Oppfolging1.id)

            consumer.consume(message.tiltaksgjennomforingId.toString(), Json.encodeToJsonElement(message))

            @Language("PostgreSQL")
            val query = """
                select nav_ident
                from gjennomforing_koordinator
                where gjennomforing_id = :gjennomforing_id::uuid
            """.trimIndent()

            val params = mapOf(
                "gjennomforing_id" to message.tiltaksgjennomforingId,
            )

            database.run {
                session.single(queryOf(query, params)) {
                    it.string("nav_ident")
                } shouldBe message.navIdent.value
            }
        }

        // TODO Test for tombstone
    }
})
