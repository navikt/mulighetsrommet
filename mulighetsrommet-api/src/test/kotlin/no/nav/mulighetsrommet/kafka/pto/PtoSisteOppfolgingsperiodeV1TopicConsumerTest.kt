package no.nav.mulighetsrommet.kafka.pto

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.services.TiltakshistorikkService
import no.nav.mulighetsrommet.api.services.VirksomhetService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.pto.PtoSisteOppfolgingsperiodeV1TopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.pto.SisteOppfolgingsperiodeV1
import java.time.ZonedDateTime
import java.util.*

class PtoSisteOppfolgingsperiodeV1TopicConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("consume") {
        val periodeUtenSlutt = SisteOppfolgingsperiodeV1(
            uuid = UUID.randomUUID(),
            aktorId = "12345678910",
            startDato = ZonedDateTime.now(),
            sluttDato = null,
        )

        val pdlClient: PdlClient = mockk()
        val virksomhetService: VirksomhetService = mockk()

        val tiltakshistorikkRepository = TiltakshistorikkRepository(database.db)
        val tiltakshistorikkService = TiltakshistorikkService(
            virksomhetService = virksomhetService,
            tiltakshistorikkRepository = tiltakshistorikkRepository,
            pdlClient = pdlClient,
        )

        coEvery { pdlClient.hentIdenter(any()) } returns listOf(
            IdentInformasjon(
                ident = "12345678910",
                gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                historisk = false,
            ),
        )
            .right()

        val oppfolgingsperiodeConsumer = PtoSisteOppfolgingsperiodeV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "oppfolgingsperiode", topic = "oppfolgingsperiode"),
            tiltakshistorikkService = tiltakshistorikkService,
            pdlClient = pdlClient,
        )

        test("1") {
            oppfolgingsperiodeConsumer.consume(periodeUtenSlutt.uuid.toString(), Json.encodeToJsonElement(periodeUtenSlutt))
        }
    }
})
