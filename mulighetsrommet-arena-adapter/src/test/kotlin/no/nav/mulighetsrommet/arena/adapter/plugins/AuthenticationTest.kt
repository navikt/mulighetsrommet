package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.plugins

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.withArenaAdapterApp
import no.nav.security.mock.oauth2.MockOAuth2Server

class AuthenticationTest : FunSpec({
    val oauth = MockOAuth2Server()
    val apiUrl = "/topics"

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("protected endpoints") {
        test("should respond with 401 when request is not authenticated") {
            withArenaAdapterApp(oauth) {
                val response = client.get(apiUrl)

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("should respond with 401 when the token has the wrong audience") {
            withArenaAdapterApp(oauth) {
                val response = client.get(apiUrl) {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${
                        oauth.issueToken(audience = "skatteetaten").serialize()
                        }"
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("should respond with 401 when the token has the wrong issuer") {
            withArenaAdapterApp(oauth) {

                val response = client.get(apiUrl) {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${
                        oauth.issueToken(issuerId = "skatteetaten").serialize()
                        }"
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("should respond with 200 when request is authenticated") {
            withArenaAdapterApp(oauth) {
                val response = client.get(apiUrl) {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer ${
                        oauth.issueToken().serialize()
                        }"
                    )
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
