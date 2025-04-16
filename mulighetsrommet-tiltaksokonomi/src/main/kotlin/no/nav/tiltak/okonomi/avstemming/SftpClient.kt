package no.nav.tiltak.okonomi.avstemming

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.APPEND
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SftpClient(val properties: SftpProperties) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    data class SftpProperties(
        val username: String,
        val host: String,
        val port: Int,
        val privateKey: String,
        val directory: String,
    )

    private val jSch = JSch()

    fun put(content: ByteArray, filename: String, alternativePort: Int?) {
        if (properties.privateKey.isNotEmpty()) {
            jSch.addIdentity(
                properties.username,
                properties.privateKey.encodeToByteArray(),
                null,
                null,
            )
        }

        var session: Session? = null
        var channel: ChannelSftp? = null
        try {
            session = jSch.getSession(properties.username, properties.host, alternativePort ?: properties.port).apply { }
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(1000)

            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.cd(properties.directory)
            channel.put(content.inputStream(), filename, APPEND)
            log.info("###### Lastet opp ny fil eller appender data til samme fil om filen finnes: {}", filename)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }
}
