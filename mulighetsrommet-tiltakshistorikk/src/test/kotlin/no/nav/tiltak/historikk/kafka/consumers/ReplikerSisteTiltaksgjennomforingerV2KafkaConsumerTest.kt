package no.nav.tiltak.historikk.kafka.consumers

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.Gjennomforing
import no.nav.tiltak.historikk.model.Virksomhet
import no.nav.tiltak.historikk.replikering.GjennomforingReplikering
import no.nav.tiltak.historikk.replikering.VirksomhetReplikering
import java.time.Instant
import java.time.LocalDate

class ReplikerSisteTiltaksgjennomforingerV2KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val gruppeAmo = TiltaksgjennomforingV2Dto.Gruppe(
        id = TestFixtures.Gjennomforing.GruppeAmo.id,
        lopenummer = Tiltaksnummer("2025/1"),
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("987654321")),
        navn = "Gruppe AMO",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = null,
        status = TiltaksgjennomforingV2Dto.Status.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.FELLES,
        tilgjengeligForArrangorFraOgMedDato = null,
        apentForPamelding = true,
        antallPlasser = 10,
        deltidsprosent = 80.0,
        oppmoteSted = null,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
        oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
    )
    val enkelAmo = TiltaksgjennomforingV2Dto.Enkeltplass(
        id = TestFixtures.Gjennomforing.EnkelAmo.id,
        lopenummer = Tiltaksnummer("2025/2"),
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("987654321")),
        status = TiltaksgjennomforingV2Dto.Status.GJENNOMFORES,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
        opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
        oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
    )

    beforeEach {
        TiltakshistorikkDatabase(database.db).session {
            queries.tiltakstype.upsert(TestFixtures.Tiltakstype.GruppeAmo)
            queries.tiltakstype.upsert(TestFixtures.Tiltakstype.EnkelAmo)
        }
    }

    afterEach {
        database.truncateAll()
    }

    context("konsumer gjennomføringer") {
        val db = TiltakshistorikkDatabase(database.db)

        val consumer = ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(
            GjennomforingReplikering(db, mockk(relaxed = true)),
        )

        val gruppe: TiltaksgjennomforingV2Dto = gruppeAmo.copy(
            opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00.123456789Z"),
            oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00.987654789Z"),
        )
        val enkeltplass: TiltaksgjennomforingV2Dto = enkelAmo

        beforeEach {
            db.session {
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arrangor)
            }
        }

        test("upsert gjennomforing from topic") {
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))
            consumer.consume(enkeltplass.id, Json.encodeToJsonElement(enkeltplass))

            db.session {
                queries.gjennomforing.get(gruppe.id) shouldBe Gjennomforing(
                    id = gruppe.id,
                    type = Gjennomforing.Type.GRUPPE,
                    tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
                    arrangorOrganisasjonsnummer = "987654321",
                    navn = "Gruppe AMO",
                    deltidsprosent = 80.0,
                    opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00.123456Z"),
                    oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00.987654Z"),
                )
                queries.gjennomforing.get(enkeltplass.id) shouldBe Gjennomforing(
                    id = enkeltplass.id,
                    type = Gjennomforing.Type.ENKELTPLASS,
                    tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                    arrangorOrganisasjonsnummer = "987654321",
                    navn = null,
                    deltidsprosent = null,
                    opprettetTidspunkt = Instant.parse("2025-01-01T00:00:00Z"),
                    oppdatertTidspunkt = Instant.parse("2025-01-02T00:00:00Z"),
                )
            }

            var updatedGruppe: TiltaksgjennomforingV2Dto = gruppeAmo.copy(
                navn = "Nytt navn",
                opprettetTidspunkt = gruppe.opprettetTidspunkt,
                oppdatertTidspunkt = Instant.parse("2025-01-03T12:34:56.123456789Z"),
            )
            consumer.consume(gruppe.id, Json.encodeToJsonElement(updatedGruppe))

            db.session {
                val lagret = queries.gjennomforing.get(gruppe.id).shouldNotBeNull()
                lagret.navn shouldBe "Nytt navn"
                lagret.opprettetTidspunkt shouldBe Instant.parse("2025-01-01T00:00:00.123456Z")
                lagret.oppdatertTidspunkt shouldBe Instant.parse("2025-01-03T12:34:56.123456Z")
            }
        }

        test("delete gjennomforing for tombstone messages") {
            db.session {
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.GruppeAmo)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.EnkelAmo)
            }

            consumer.consume(gruppe.id, JsonNull)

            db.session {
                queries.gjennomforing.get(gruppe.id).shouldBeNull()
                queries.gjennomforing.get(enkeltplass.id).shouldNotBeNull()
            }

            consumer.consume(enkeltplass.id, JsonNull)

            db.session {
                queries.gjennomforing.get(gruppe.id).shouldBeNull()
                queries.gjennomforing.get(enkeltplass.id).shouldBeNull()
            }
        }
    }

    context("synkroniserer virksomhet hvis den ikke finnes") {
        val db = TiltakshistorikkDatabase(database.db)

        val gruppe: TiltaksgjennomforingV2Dto = gruppeAmo

        test("lagrer virksomhet fra brreg") {
            var brreg = mockk<BrregClient>()
            coEvery { brreg.getBrregEnhet(Organisasjonsnummer("987654321")) } returns BrregHovedenhetDto(
                organisasjonsnummer = Organisasjonsnummer("987654321"),
                organisasjonsform = "BEDR",
                navn = "Arrangør",
                postadresse = null,
                forretningsadresse = null,
                overordnetEnhet = null,
            ).right()
            var virksomheter = VirksomhetReplikering(db, brreg)

            val consumer = ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(GjennomforingReplikering(db, virksomheter))
            consumer.consume(gruppe.id, Json.encodeToJsonElement(gruppe))

            db.session {
                queries.gjennomforing.get(gruppe.id)?.arrangorOrganisasjonsnummer shouldBe "987654321"
            }

            virksomheter.getVirksomhet(Organisasjonsnummer("987654321")).shouldBe(TestFixtures.Virksomhet.Arrangor)
        }

        test("lagrer utenlandsk virksomhet uten navn") {
            var brreg = mockk<BrregClient>()
            var virksomheter = VirksomhetReplikering(db, brreg)

            var gjennomforing: TiltaksgjennomforingV2Dto = gruppeAmo.copy(
                arrangor = TiltaksgjennomforingV2Dto.Arrangor(Organisasjonsnummer("111222333")),
            )

            val consumer = ReplikerSisteTiltaksgjennomforingerV2KafkaConsumer(GjennomforingReplikering(db, virksomheter))
            consumer.consume(gjennomforing.id, Json.encodeToJsonElement(gjennomforing))

            db.session {
                queries.gjennomforing.get(gjennomforing.id)?.arrangorOrganisasjonsnummer shouldBe "111222333"
            }

            virksomheter.getVirksomhet(Organisasjonsnummer("111222333")) shouldBe Virksomhet(
                organisasjonsnummer = Organisasjonsnummer("111222333"),
                overordnetEnhetOrganisasjonsnummer = null,
                navn = null,
                organisasjonsform = null,
                slettetDato = null,
            )
        }
    }
})
