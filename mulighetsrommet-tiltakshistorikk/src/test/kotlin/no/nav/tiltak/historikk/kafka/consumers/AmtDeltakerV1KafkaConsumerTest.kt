package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AmtDeltakerV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("consume deltakere") {
        val db = TiltakshistorikkDatabase(database.db)

        val deltakerConsumer = AmtDeltakerV1KafkaConsumer(db)

        val tiltak = TiltaksgjennomforingEksternV1Dto(
            id = UUID.randomUUID(),
            tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Gruppe AMO",
                arenaKode = "GRUPPEAMO",
                tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            ),
            navn = "Gruppe AMO",
            virksomhetsnummer = "123123123",
            startDato = LocalDate.now(),
            sluttDato = null,
            status = GjennomforingStatus.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.FELLES,
            tilgjengeligForArrangorFraOgMedDato = null,
            apentForPamelding = true,
            antallPlasser = 10,
            opprettetTidspunkt = LocalDateTime.now(),
            oppdatertTidspunkt = LocalDateTime.now(),
        )

        val deltakelsesdato = LocalDateTime.of(2023, 3, 1, 0, 0, 0)

        val amtDeltaker1 = AmtDeltakerV1Dto(
            id = UUID.randomUUID(),
            gjennomforingId = tiltak.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = DeltakerStatus(
                type = DeltakerStatus.Type.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = deltakelsesdato,
            ),
            registrertDato = deltakelsesdato,
            endretDato = deltakelsesdato,
            dagerPerUke = 2.5f,
            prosentStilling = null,
            deltakelsesmengder = listOf(),
        )

        beforeEach {
            db.session {
                queries.gruppetiltak.upsert(tiltak)
            }
        }

        afterEach {
            database.truncateAll()
        }

        test("upsert deltakere from topic") {
            deltakerConsumer.consume(amtDeltaker1.id, Json.encodeToJsonElement(amtDeltaker1))

            database.assertTable("komet_deltaker")
                .row()
                .value("id").isEqualTo(amtDeltaker1.id)
        }

        test("delete deltakere for tombstone messages") {
            db.session {
                queries.deltaker.upsertKometDeltaker(amtDeltaker1)
            }

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            database.assertTable("komet_deltaker").isEmpty
        }

        test("delete deltakere that have status FEILREGISTRERT") {
            db.session {
                queries.deltaker.upsertKometDeltaker(amtDeltaker1)
            }

            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                status = DeltakerStatus(
                    type = DeltakerStatus.Type.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            database.assertTable("komet_deltaker").isEmpty
        }
    }
})
