package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.kafka.dto.TiltakAvtaleHendelseDto
import no.nav.tiltak.historikk.model.ArbeidsgiverAvtale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ReplikerTiltakAvtaleKafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("consume tiltak-avtale-hendelser") {
        val db = TiltakshistorikkDatabase(database.db)
        val consumer = ReplikerTiltakAvtaleKafkaConsumer(db)

        val avtaleId = UUID.randomUUID()
        val sistEndret = Instant.parse("2024-10-03T08:14:07.989329Z")

        val hendelse = TiltakAvtaleHendelseDto(
            avtaleId = avtaleId,
            deltakerFnr = NorskIdent("24518549827"),
            bedriftNr = "910825518",
            tiltakstype = TiltakAvtaleHendelseDto.Tiltakstype.ARBEIDSTRENING,
            startDato = LocalDate.of(2024, 10, 3),
            sluttDato = LocalDate.of(2025, 1, 2),
            avtaleStatus = TiltakAvtaleHendelseDto.Status.GJENNOMFORES,
            stillingprosent = 100f,
            antallDagerPerUke = 5f,
            opprettetTidspunkt = LocalDateTime.of(2024, 10, 3, 10, 10, 1, 271029000),
            sistEndret = sistEndret,
        )
        val organisasjonsnummer = Organisasjonsnummer(hendelse.bedriftNr)

        afterEach {
            database.truncateAll()
        }

        test("lagrer ny avtale fra topic") {
            consumer.consume(avtaleId, Json.encodeToJsonElement(hendelse))

            db.session {
                val lagret = queries.arbeidsgiverAvtale.get(avtaleId)
                lagret?.avtaleId shouldBe avtaleId
                lagret?.status shouldBe ArbeidsgiverAvtale.Status.GJENNOMFORES
            }
        }

        test("sletter avtale ved tombstone-melding") {
            db.session {
                queries.arbeidsgiverAvtale.upsert(hendelse.toArbeidsgiverAvtale(organisasjonsnummer))
            }

            consumer.consume(avtaleId, JsonNull)

            db.session {
                queries.arbeidsgiverAvtale.get(avtaleId).shouldBeNull()
            }
        }

        test("hopper over avtale hvis innkommende hendelse har et ugyldig bedriftNr") {
            val hendelse = hendelse.copy(bedriftNr = "")
            consumer.consume(avtaleId, Json.encodeToJsonElement(hendelse))

            db.session {
                queries.arbeidsgiverAvtale.get(avtaleId).shouldBeNull()
            }
        }

        test("hopper over avtale hvis innkommende sistEndret er eldre enn lagret") {
            db.session {
                queries.arbeidsgiverAvtale.upsert(hendelse.toArbeidsgiverAvtale(organisasjonsnummer))
            }

            val utdatertHendelse = hendelse.copy(
                sistEndret = sistEndret.minusSeconds(1),
                startDato = LocalDate.of(2023, 1, 1),
            )
            consumer.consume(avtaleId, Json.encodeToJsonElement(utdatertHendelse))

            db.session {
                queries.arbeidsgiverAvtale.get(avtaleId)?.startDato shouldBe hendelse.startDato
            }
        }

        test("oppdaterer avtale hvis innkommende sistEndret er nyere enn lagret") {
            db.session {
                queries.arbeidsgiverAvtale.upsert(hendelse.toArbeidsgiverAvtale(organisasjonsnummer))
            }

            val oppdatertHendelse = hendelse.copy(
                sistEndret = sistEndret.plusSeconds(1),
                startDato = LocalDate.of(2025, 3, 1),
            )
            consumer.consume(avtaleId, Json.encodeToJsonElement(oppdatertHendelse))

            db.session {
                queries.arbeidsgiverAvtale.get(avtaleId)?.startDato shouldBe oppdatertHendelse.startDato
            }
        }
    }
})
