package no.nav.mulighetsrommet.kafka.pto

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.pto.PtoSisteOppfolgingsperiodeV1TopicConsumer
import no.nav.mulighetsrommet.kafka.consumers.pto.SisteOppfolgingsperiodeV1
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class PtoSisteOppfolgingsperiodeV1TopicConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val pdlClient: PdlClient = mockk()
    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
            NavEnhetFixtures.Innlandet,
            NavEnhetFixtures.Gjovik,
            NavEnhetFixtures.IT,
        ),
        tiltakstyper = listOf(
            TiltakstypeFixtures.Oppfolging,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    context("context") {
        val tiltakshistorikkRepository = TiltakshistorikkRepository(database.db)

        val oppfolgingsperiodeConsumer = PtoSisteOppfolgingsperiodeV1TopicConsumer(
            config = KafkaTopicConsumer.Config(id = "oppfolgingsperiode", topic = "oppfolgingsperiode"),
            tiltakshistorikk = tiltakshistorikkRepository,
            pdlClient = pdlClient,
        )

        test("periode uten slutt sletter ikke") {
            tiltakshistorikkRepository.upsert(
                ArenaTiltakshistorikkDbo.Gruppetiltak(
                    id = UUID.randomUUID(),
                    norskIdent = NorskIdent("12345678910"),
                    status = Deltakerstatus.DELTAR,
                    fraDato = LocalDateTime.now(),
                    tilDato = null,
                    registrertIArenaDato = LocalDateTime.now(),
                    tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                ),
            )

            val periodeUtenSlutt = SisteOppfolgingsperiodeV1(
                uuid = UUID.randomUUID(),
                aktorId = "12345678910",
                startDato = ZonedDateTime.now(),
                sluttDato = null,
            )

            coEvery { pdlClient.hentHistoriskeIdenter(any(), any()) } returns listOf(
                IdentInformasjon(
                    ident = PdlIdent("12345678910"),
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    historisk = false,
                ),
            )
                .right()

            oppfolgingsperiodeConsumer.consume(
                periodeUtenSlutt.uuid.toString(),
                Json.encodeToJsonElement(periodeUtenSlutt),
            )
            tiltakshistorikkRepository.getTiltakshistorikkForBruker(listOf(NorskIdent("12345678910"))).shouldHaveSize(1)
        }

        test("periode med slutt sletter") {
            tiltakshistorikkRepository.upsert(
                ArenaTiltakshistorikkDbo.Gruppetiltak(
                    id = UUID.randomUUID(),
                    norskIdent = NorskIdent("12345678910"),
                    status = Deltakerstatus.DELTAR,
                    fraDato = LocalDateTime.now(),
                    tilDato = null,
                    registrertIArenaDato = LocalDateTime.now(),
                    tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                ),
            )

            val periodeUtenSlutt = SisteOppfolgingsperiodeV1(
                uuid = UUID.randomUUID(),
                aktorId = "12345678910",
                startDato = ZonedDateTime.now(),
                sluttDato = ZonedDateTime.now(),
            )

            coEvery { pdlClient.hentHistoriskeIdenter(any(), any()) } returns listOf(
                IdentInformasjon(
                    ident = PdlIdent("12345678910"),
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    historisk = false,
                ),
            )
                .right()

            oppfolgingsperiodeConsumer.consume(
                periodeUtenSlutt.uuid.toString(),
                Json.encodeToJsonElement(periodeUtenSlutt),
            )
            tiltakshistorikkRepository.getTiltakshistorikkForBruker(listOf(NorskIdent("12345678910"))).shouldHaveSize(0)
        }
    }
})
