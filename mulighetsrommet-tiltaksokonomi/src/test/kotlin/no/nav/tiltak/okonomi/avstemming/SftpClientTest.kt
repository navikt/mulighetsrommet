package no.nav.tiltak.okonomi.avstemming

import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer
import io.kotest.core.spec.style.FunSpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SftpClientTest : FunSpec({
    test("skal kunne laste opp filinnhold") {
        val sftpClient = SftpClient(
            SftpClient.SftpProperties(
                username = "username",
                directory = "directory",
                host = "",
                port = 0,
                privateKey = "",
            ),
        )

        withSftpServer { server ->
            server.addUser(sftpClient.properties.username, "")
            server.createDirectory(sftpClient.properties.directory)
            val content = "T-2918-1;Refusjon;2200;20250112;123456789;123456789".toByteArray()
            val filename = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_LT_tiltak_avstemming_dag.csv"
            sftpClient.put(content, filename, server.port)
        }
    }
})
