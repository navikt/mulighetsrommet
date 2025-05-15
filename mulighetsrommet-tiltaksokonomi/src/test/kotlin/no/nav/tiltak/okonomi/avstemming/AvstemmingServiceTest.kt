package no.nav.tiltak.okonomi.avstemming

import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.okonomi.*
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.text.Charsets.UTF_8

class AvstemmingServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val sftpClient = SftpClient(
        SftpClient.SftpProperties(
            username = "username",
            directory = "directory",
            host = "",
            port = 0,
            privateKey = "",
        ),
    )

    val bestilling = Bestilling.fromOpprettBestilling(
        OpprettBestilling(
            bestillingsnummer = "1",
            tilskuddstype = Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
            tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
            arrangor = OpprettBestilling.Arrangor(
                hovedenhet = Organisasjonsnummer("123456789"),
                underenhet = Organisasjonsnummer("234567891"),
            ),
            avtalenummer = null,
            belop = 1000,
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            kostnadssted = NavEnhetNummer("0400"),
        ),
    )

    val faktura = Faktura.fromOpprettFaktura(
        OpprettFaktura(
            fakturanummer = "1-1",
            bestillingsnummer = "1",
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = Kontonummer("12345678901"),
                kid = null,
            ),
            belop = 1000,
            periode = Periode.forMonthOf(LocalDate.of(2025, 1, 1)),
            behandletAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            behandletTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDate.of(2025, 1, 1).atStartOfDay(),
            gjorOppBestilling = false,
            beskrivelse = "Beskrivelse",
        ),
        bestillingLinjer = bestilling.linjer,
    )

    test("avstemmingsfil har riktig innhold") {
        val db = OkonomiDatabase(database.db)
        val avstemmingService = AvstemmingService(
            db,
            sftpClient,
        )
        val fakturaCsvData = db.session {
            queries.bestilling.insertBestilling(bestilling)
            queries.faktura.insertFaktura(faktura)

            queries.faktura.getNotAvstemt()[0]
        }

        FakeSftpServer.withSftpServer { server ->
            server.addUser(sftpClient.properties.username, "")
            server.createDirectory(sftpClient.properties.directory)

            val tidspunkt = LocalDateTime.now()
            avstemmingService.avstem(server.port)

            val fileContent = server.getFileContent("${sftpClient.properties.directory}/${AvstemmingService.filename(tidspunkt)}", UTF_8)
            fileContent.isEmpty() shouldBe false
            fileContent.lines().size shouldBe 2
            fileContent.lines() shouldContain bestilling.toCSVRad()
            fileContent.lines() shouldContain fakturaCsvData.toCSVRad()
        }
        db.session { queries.faktura.getNotAvstemt() }.size shouldBe 0
        db.session { queries.bestilling.getNotAvstemt() }.size shouldBe 0
    }
})
