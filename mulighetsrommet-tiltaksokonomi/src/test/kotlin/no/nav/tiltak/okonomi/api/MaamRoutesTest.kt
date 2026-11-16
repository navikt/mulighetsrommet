package no.nav.tiltak.okonomi.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.maam.dto.DeleteKafkaRecordRequest
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltak.okonomi.databaseConfig
import no.nav.tiltak.okonomi.withTestApplication

class MaamRoutesTest : FunSpec({
    extension(FlywayDatabaseTestListener(databaseConfig))
    val oauth = MockOAuth2Server()

    beforeSpec { oauth.start() }
    afterSpec { oauth.shutdown() }

    test("sletting krever autentisering") {
        withTestApplication(oauth) {
            client.delete("/maam/kafka/failed-records")
                .status shouldBe HttpStatusCode.Unauthorized
        }
    }

    test("sletteruten er tilgjengelig med gyldig token") {
        withTestApplication(oauth) {
            client.delete("/maam/kafka/failed-records") {
                bearerAuth(oauth.issueToken().serialize())
                contentType(ContentType.Application.Json)
                setBody(DeleteKafkaRecordRequest(id = 0, topic = "missing"))
            }.status shouldBe HttpStatusCode.Conflict
        }
    }
})
