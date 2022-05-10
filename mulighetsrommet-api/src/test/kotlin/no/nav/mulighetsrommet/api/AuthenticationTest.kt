package no.nav.mulighetsrommet.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.security.mock.oauth2.MockOAuth2Server

class AuthenticationTest : FunSpec({
//
//    val oauth = MockOAuth2Server()
//
//    beforeSpec {
//        oauth.start()
//    }
//
//    afterSpec {
//        oauth.shutdown()
//    }
//
//    context("protected endpoints") {
//        test("should respond with 401 when request is not authenticated") {
//            withMulighetsrommetApp(oauth) {
//                handleRequest(HttpMethod.Get, "/api/innsatsgrupper").run {
//                    response.status() shouldBe HttpStatusCode.Unauthorized
//                }
//            }
//        }
//
//        test("should respond with 401 when the token has the wrong audience") {
//            withMulighetsrommetApp(oauth) {
//                handleRequest(HttpMethod.Get, "/api/innsatsgrupper") {
//                    addHeader(
//                        HttpHeaders.Authorization,
//                        "Bearer ${oauth.issueToken(audience = "skatteetaten").serialize()}"
//                    )
//                }.run {
//                    response.status() shouldBe HttpStatusCode.Unauthorized
//                }
//            }
//        }
//
//        test("should respond with 401 when the token has the wrong issuer") {
//            withMulighetsrommetApp(oauth) {
//                handleRequest(HttpMethod.Get, "/api/innsatsgrupper") {
//                    addHeader(
//                        HttpHeaders.Authorization,
//                        "Bearer ${oauth.issueToken(issuerId = "skatteetaten").serialize()}"
//                    )
//                }.run {
//                    response.status() shouldBe HttpStatusCode.Unauthorized
//                }
//            }
//        }
//
//        test("should respond with 200 when request is authenticated") {
//            withMulighetsrommetApp(oauth) {
//                handleRequest(HttpMethod.Get, "/api/innsatsgrupper") {
//                    addHeader(HttpHeaders.Authorization, "Bearer ${oauth.issueToken().serialize()}")
//                }.run {
//                    response.status() shouldBe HttpStatusCode.OK
//                }
//            }
//        }
//    }
})
