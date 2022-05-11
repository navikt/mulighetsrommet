package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.Masked
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArenaOrdsRoutesTest : FunSpec({

    val mockArenaOrdsClient = mockk<ArenaOrdsClient>()

    context("person") {

        test("should return a populated person list with fnr") {
            val testInput = ArenaPersonIdList(listOf(PersonFnr(123), PersonFnr(456)))
            val mockReturn = ArenaPersonIdList(listOf(PersonFnr(123, "123"), PersonFnr(456, "456")))
            coEvery { mockArenaOrdsClient.getFnrByArenaPersonId(any()) } returns mockReturn
            withArenaOrdsProxyApp(mockArenaOrdsClient) {

                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.post("/api/person") {
                    contentType(ContentType.Application.Json)
                    setBody(testInput)
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe Json.encodeToString(mockReturn)
            }
        }
    }

    context("arbeidsgiver") {

        test("should return arbeidsgiverinfo") {
            val mockReturn = ArbeidsgiverInfo(123, 456)
            coEvery { mockArenaOrdsClient.getArbeidsgiverInfoByArenaArbeidsgiverId(any()) } returns mockReturn
            withArenaOrdsProxyApp(mockArenaOrdsClient) {

                val client = createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

                val response = client.get("/api/arbeidsgiver/asdasasd") {
                    contentType(ContentType.Application.Json)
                }

                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe Json.encodeToString(mockReturn)
            }
        }
    }

})
