package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server

class AuthenticationTest : FunSpec({

    val oauth = MockOAuth2Server()
    val apiUrl = "/api/v1/innsatsgrupper"

    beforeSpec {
        oauth.start()
    }

    afterSpec {
        oauth.shutdown()
    }

    context("protected endpoints") {
        test("should respond with 401 when request is not authenticated") {
            withMulighetsrommetApp(oauth) {
                val response = client.get(apiUrl)

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("should respond with 401 when the token has the wrong audience") {
            withMulighetsrommetApp(oauth) {
                val response = client.get(apiUrl) {
                    header(HttpHeaders.Authorization, "Bearer ${oauth.issueToken(audience = "skatteetaten").serialize()}")
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("should respond with 401 when the token has the wrong issuer") {
            withMulighetsrommetApp(oauth) {

                val response = client.get(apiUrl) {
                    header(HttpHeaders.Authorization, "Bearer ${oauth.issueToken(issuerId = "skatteetaten").serialize()}")
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("should respond with 200 when request is authenticated") {
            withMulighetsrommetApp(oauth) {
                val response = client.get(apiUrl) {
                    header(HttpHeaders.Authorization, "Bearer ${oauth.issueToken().serialize()}")
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
